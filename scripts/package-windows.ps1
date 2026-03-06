param(
    [ValidateSet("app-image", "exe", "msi")]
    [string]$Type = "app-image",
    [string]$AppVersion = "0.1.0",
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$jdkCandidates = @(
    (Join-Path $projectRoot ".tools/jdk/jdk-21.0.10+7"),
    $env:JAVA_HOME
) | Where-Object { $_ -and (Test-Path $_) }

if (-not $jdkCandidates) {
    throw "Java 21 JDK was not found. Set JAVA_HOME or place the local JDK in .tools/jdk/."
}

$jdkHome = $jdkCandidates[0]
$env:JAVA_HOME = $jdkHome
$env:PATH = "$jdkHome\bin;$env:PATH"

if ($Type -eq 'exe' -or $Type -eq 'msi') {
    $localWix = Join-Path $projectRoot '.tools/wix'
    if ((Test-Path (Join-Path $localWix 'candle.exe')) -and (Test-Path (Join-Path $localWix 'light.exe'))) {
        $env:DATALENS_WIX_REAL_LIGHT = Join-Path $localWix 'light-real.exe'
        $env:DATALENS_WIX_LIGHT_EXTRA = '-sval'
        $env:PATH = "$localWix;$env:PATH"
    }

    if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue) -or -not (Get-Command light.exe -ErrorAction SilentlyContinue)) {
        throw "WiX Toolset was not found. Run scripts/install-wix.ps1 or install WiX system-wide for exe/msi packaging."
    }
}

$fatJar = Join-Path $projectRoot 'target/datalens-0.1.0-SNAPSHOT-fat.jar'
if ($Rebuild -or -not (Test-Path $fatJar)) {
    $mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mavenCmd) {
        $mavenCmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    }

    if (-not $mavenCmd) {
        $localMaven = Join-Path $projectRoot '.tools/maven/apache-maven-3.9.12/bin/mvn.cmd'
        if (Test-Path $localMaven) {
            $mavenCmd = Get-Item $localMaven
        }
    }

    if (-not $mavenCmd) {
        throw "Maven was not found. Install Maven or place it in .tools/maven/."
    }

    & $mavenCmd.Source clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed."
    }
}

$jpackage = Join-Path $jdkHome 'bin/jpackage.exe'
if (-not (Test-Path $jpackage)) {
    throw "jpackage.exe was not found in the selected JDK."
}

$distDir = Join-Path $projectRoot 'dist'
$tempRoot = Join-Path $projectRoot '.jpackage-temp'
$packageTemp = Join-Path $tempRoot $Type
$outputPath = if ($Type -eq 'app-image') {
    Join-Path $distDir 'DataLens'
} else {
    Join-Path $distDir ("DataLens-$AppVersion.$Type")
}
New-Item -ItemType Directory -Force -Path $distDir,$tempRoot | Out-Null
if (Test-Path $packageTemp) {
    Remove-Item -Recurse -Force $packageTemp
}
if (Test-Path $outputPath) {
    Remove-Item -Recurse -Force $outputPath
}
New-Item -ItemType Directory -Force -Path $packageTemp | Out-Null

$args = @(
    '--type', $Type,
    '--name', 'DataLens',
    '--dest', $distDir,
    '--temp', $packageTemp,
    '--input', (Join-Path $projectRoot 'target'),
    '--main-jar', 'datalens-0.1.0-SNAPSHOT-fat.jar',
    '--main-class', 'com.datalens.app.MainApp',
    '--app-version', $AppVersion,
    '--vendor', 'DataLens'
)

if ($Type -ne 'app-image') {
    $args += @('--win-shortcut', '--win-dir-chooser')
}

& $jpackage @args

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed."
}

Write-Host "Packaging finished in $distDir"

