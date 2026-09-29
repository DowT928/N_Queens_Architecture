# Shared contract and CLI checks. AI: Codex generated; owner review pending.
param([string]$JavaHome)
$ErrorActionPreference = 'Stop'
$temporary = $null
try {
    . (Join-Path $PSScriptRoot 'common.ps1')
    $jdk = Resolve-Jdk $JavaHome
    $temporary = New-UniqueDirectory (Join-Path $script:Root 'build')
    $sources = @(Get-ChildItem (Join-Path $script:Root 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    $compile = @('-J-Dfile.encoding=UTF-8','--release','17','-encoding','UTF-8','-d',$temporary) + $sources + @((Join-Path $script:Root 'tests\ContractChecks.java'))
    if ((Invoke-Captured $jdk.Javac $compile $temporary 'compile-') -ne 0) { throw 'Compilation failed' }
    if ((Invoke-Captured $jdk.Java @('-cp',$temporary,'ContractChecks') $temporary 'contract-') -ne 0) { throw 'Contract checks failed' }
    foreach ($id in @('pipefilter','callreturn-iterative','callreturn-backtracking','blackboard','mapreduce')) {
        $code = Invoke-Captured $jdk.Java @('-cp',$temporary,'nqueens.cli.Main','--architecture',$id,'--n','8') $temporary ($id + '-')
        if ($code -notin @(0,3)) { throw "Unexpected exit for $id : $code" }
        $result = Get-Content -LiteralPath (Join-Path $temporary ($id + '-stdout.log')) -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($result.architecture -ne $id -or $result.n -ne 8) { throw 'CLI dispatch mismatch' }
        if (($code -eq 3 -and $result.status -ne 'not_implemented') -or ($code -eq 0 -and $result.status -ne 'solved')) { throw 'CLI status mismatch' }
    }
    $index = 0
    foreach ($argsSet in @(
        @('--architecture','bad','--n','8'),
        @('--architecture','pipefilter','--n','0'),
        @('--architecture','pipefilter','--n','16'),
        @('--architecture','pipefilter','--n','abc'),
        @('--architecture','pipefilter')
    )) {
        $code = Invoke-Captured $jdk.Java (@('-cp',$temporary,'nqueens.cli.Main') + $argsSet) $temporary ("invalid-$index-")
        if ($code -ne 2) { throw 'Invalid arguments must return 2' }
        $index++
    }
    Write-Host 'All contract and CLI checks passed.'
} finally {
    if ($temporary -and (Test-Path -LiteralPath $temporary)) {
        $resolved = [IO.Path]::GetFullPath($temporary)
        $allowed = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\build')) + '\'
        if (-not $resolved.StartsWith($allowed, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe cleanup path' }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
