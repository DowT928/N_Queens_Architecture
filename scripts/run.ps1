# Daily debug entry. AI: Codex generated; owner review pending.
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('pipefilter','callreturn-iterative','callreturn-backtracking','blackboard','mapreduce')]
    [string]$Architecture,
    [Parameter(Mandatory=$true)][ValidateRange(1,15)][int]$N,
    [string]$JavaHome,
    [string[]]$JvmArgs = @()
)
try {
    . (Join-Path $PSScriptRoot 'common.ps1')
    $code = Invoke-Session $Architecture @($N) 1 $JavaHome $JvmArgs $false
    exit $code
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}
