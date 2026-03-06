param(
    [string]$VersionTag = 'wix3141rtm',
    [string]$ArchiveName = 'wix314-binaries.zip'
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
$downloadsRoot = Join-Path $toolsRoot 'downloads'
$wixRoot = Join-Path $toolsRoot 'wix'
$archivePath = Join-Path $downloadsRoot $ArchiveName
$url = "https://github.com/wixtoolset/wix3/releases/download/$VersionTag/$ArchiveName"

New-Item -ItemType Directory -Force -Path $downloadsRoot,$wixRoot | Out-Null

Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $archivePath

Get-ChildItem $wixRoot -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force
Expand-Archive -Path $archivePath -DestinationPath $wixRoot -Force

$required = @('candle.exe', 'light.exe')
foreach ($tool in $required) {
    if (-not (Test-Path (Join-Path $wixRoot $tool))) {
        throw "WiX installation is incomplete. Missing $tool."
    }
}

Write-Host "WiX Toolset extracted to $wixRoot"