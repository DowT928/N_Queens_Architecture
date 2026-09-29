# Shared build/capture infrastructure. AI: Codex generated; owner review pending.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:Root = Split-Path -Parent $PSScriptRoot
$script:Utf8 = New-Object System.Text.UTF8Encoding($false)
if (-not ('NQueensCapture' -as [type])) {
    Add-Type -Path (Join-Path $PSScriptRoot 'ProcessCapture.cs')
}

function Write-NewText([string]$Path, [string]$Text) {
    $bytes = $script:Utf8.GetBytes($Text + [Environment]::NewLine)
    $stream = [IO.File]::Open($Path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
    try { $stream.Write($bytes, 0, $bytes.Length) } finally { $stream.Dispose() }
}
function New-UniqueDirectory([string]$Parent) {
    $id = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ') + '-' + [Guid]::NewGuid().ToString('N')
    $path = Join-Path $Parent $id
    New-Item -ItemType Directory -Path $path -ErrorAction Stop | Out-Null
    return $path
}
function Resolve-Jdk([string]$JavaHome) {
    if ($JavaHome) { $jdk = $JavaHome }
    elseif ($env:JAVA_HOME) { $jdk = $env:JAVA_HOME }
    else {
        $javaCommand = Get-Command java.exe -ErrorAction Stop
        $jdk = Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
    }
    $jdk = (Resolve-Path -LiteralPath $jdk).Path
    $java = Join-Path $jdk 'bin\java.exe'
    $javac = Join-Path $jdk 'bin\javac.exe'
    if (!(Test-Path -LiteralPath $java) -or !(Test-Path -LiteralPath $javac)) {
        throw 'Selected JavaHome must contain bin/java.exe and bin/javac.exe.'
    }
    return @{ Java = $java; Javac = $javac }
}
function Quote-PowerShell([string]$Value) {
    return "'" + $Value.Replace("'", "''") + "'"
}
function Quote-Native([string]$Value) {
    # Windows CommandLineToArgvW quoting, including trailing backslashes.
    return '"' + [regex]::Replace([regex]::Replace($Value, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
}
function Invoke-Captured([string]$Executable, [string[]]$Arguments, [string]$Directory, [string]$Prefix) {
    $command = '& ' + (Quote-PowerShell $Executable) + ' ' + (($Arguments | ForEach-Object { Quote-PowerShell $_ }) -join ' ')
    Write-NewText (Join-Path $Directory ($Prefix + 'command.txt')) ("Set-Location -LiteralPath " + (Quote-PowerShell $script:Root) + [Environment]::NewLine + $command)
    $nativeArguments = ($Arguments | ForEach-Object { Quote-Native $_ }) -join ' '
    try {
        $code = [NQueensCapture]::Run($Executable, $nativeArguments, $script:Root,
            (Join-Path $Directory ($Prefix + 'stdout.log')), (Join-Path $Directory ($Prefix + 'stderr.log')))
    } catch {
        # Collector diagnostics are separate from the untouched child streams.
        Write-NewText (Join-Path $Directory ($Prefix + 'capture-error.txt')) $_.Exception.ToString()
        $code = 1
    }
    Write-NewText (Join-Path $Directory ($Prefix + 'exit-code.txt')) ([string]$code)
    return [int]$code
}
function Get-VersionText([string]$Executable) {
    $p = New-Object Diagnostics.Process
    $p.StartInfo = New-Object Diagnostics.ProcessStartInfo
    $p.StartInfo.FileName = $Executable
    $p.StartInfo.Arguments = '-version'
    $p.StartInfo.UseShellExecute = $false
    $p.StartInfo.CreateNoWindow = $true
    $p.StartInfo.RedirectStandardOutput = $true
    $p.StartInfo.RedirectStandardError = $true
    try {
        [void]$p.Start()
        $a = $p.StandardOutput.ReadToEndAsync()
        $b = $p.StandardError.ReadToEndAsync()
        $p.WaitForExit()
        if ($p.ExitCode -ne 0) { throw "Cannot query version: $Executable" }
        return ($a.Result + $b.Result).Trim()
    } finally { $p.Dispose() }
}
function Save-Environment($Jdk, [string]$Directory) {
    $os = Get-CimInstance Win32_OperatingSystem
    $cpu = @(Get-CimInstance Win32_Processor)
    $system = Get-CimInstance Win32_ComputerSystem
    $lines = @(
        "OS: $($os.Caption) $($os.Version) $($os.OSArchitecture)"
        "CPU: $(($cpu | ForEach-Object { $_.Name.Trim() }) -join '; ')"
        "Memory bytes: $($system.TotalPhysicalMemory)"
        "Java: $(Get-VersionText $Jdk.Java)"
        "Javac: $(Get-VersionText $Jdk.Javac)"
    )
    foreach ($name in @('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVAC_OPTIONS', 'CLASSPATH')) {
        $value = [Environment]::GetEnvironmentVariable($name)
        if ($value) { $lines += "${name}: $value" }
    }
    Write-NewText (Join-Path $Directory 'environment.txt') ($lines -join [Environment]::NewLine)
}
function Assert-CleanRepository {
    & git -C $script:Root rev-parse --verify HEAD 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Formal experiments require committed code.' }
    $status = & git -C $script:Root status --porcelain --untracked-files=all
    if ($LASTEXITCODE -ne 0 -or $status) { throw 'Commit all changes before formal experiments (including earlier raw logs).' }
}
function Invoke-Session([string]$Architecture, [int[]]$Sizes, [int]$Repeat, [string]$JavaHome, [string[]]$JvmArgs, [bool]$Formal) {
    if ($Formal) { Assert-CleanRepository }
    $jdk = Resolve-Jdk $JavaHome
    $category = if ($Formal) { 'raw' } else { 'debug' }
    $directory = New-UniqueDirectory (Join-Path $script:Root "experiments\$category\$Architecture")
    Write-Host "Logs: $directory"
    Save-Environment $jdk $directory
    $build = New-UniqueDirectory (Join-Path $script:Root 'build')
    $sources = @(Get-ChildItem -LiteralPath (Join-Path $script:Root 'src\main\java') -Filter '*.java' -Recurse | Sort-Object FullName | ForEach-Object { $_.FullName })
    $compileArgs = @('-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-d', $build) + $sources
    $code = Invoke-Captured $jdk.Javac $compileArgs $directory 'compile-'
    if ($code -ne 0) { return 1 }
    $failed = 0
    foreach ($n in $Sizes) {
        for ($iteration = 1; $iteration -le $Repeat; $iteration++) {
            $runDirectory = Join-Path $directory ("N{0}-run{1:D2}" -f $n, $iteration)
            New-Item -ItemType Directory -Path $runDirectory | Out-Null
            $runArgs = @($JvmArgs) + @('-cp', $build, 'nqueens.cli.Main', '--architecture', $Architecture, '--n', [string]$n)
            $code = Invoke-Captured $jdk.Java $runArgs $runDirectory ''
            if ($code -ne 0) { $failed = $code }
        }
    }
    return $failed
}
