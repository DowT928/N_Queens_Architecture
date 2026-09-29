# Formal batch entry. AI: Codex generated; owner review pending.
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('pipefilter','callreturn-iterative','callreturn-backtracking','blackboard','mapreduce')]
    [string]$Architecture,
    [ValidateRange(1,1000)][int]$Repeat = 5,
    [string]$JavaHome,
    [string[]]$JvmArgs = @()
)
try {
    . (Join-Path $PSScriptRoot 'common.ps1')
    $code = Invoke-Session $Architecture @(8,10,12) $Repeat $JavaHome $JvmArgs $true
    exit $code
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}
