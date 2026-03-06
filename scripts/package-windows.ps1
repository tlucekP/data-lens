param(
    [ValidateSet("app-image", "exe", "msi")]
    [string]$Type = "app-image",
    [string]$AppVersion,
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

[xml]$pom = Get-Content (Join-Path $projectRoot 'pom.xml')
$artifactId = $pom.project.artifactId
$projectVersion = $pom.project.version

if ([string]::IsNullOrWhiteSpace($artifactId) -or [string]::IsNullOrWhiteSpace($projectVersion)) {
    throw "Could not resolve artifactId/version from pom.xml."
}

if ([string]::IsNullOrWhiteSpace($AppVersion)) {
    $AppVersion = ($projectVersion -replace '-.*$', '')
}

$jdkCandidates = @(
    (Join-Path $projectRoot ".tools/jdk/jdk-21.0.10+7"),
    $env:JAVA_HOME
) | Where-Object { $_ -and (Test-Path $_) }
$jdkCandidates = @($jdkCandidates)

if ($jdkCandidates.Count -eq 0) {
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

$targetDir = Join-Path $projectRoot 'target'

function Resolve-FatJar {
    param(
        [string]$Directory,
        [string]$ExpectedPrefix,
        [switch]$AllowMissing
    )

    $fatJars = @(Get-ChildItem -Path $Directory -Filter '*-fat.jar' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.BaseName -like "$ExpectedPrefix*" })

    if ($fatJars.Count -eq 1) {
        return $fatJars[0]
    }

    if ($fatJars.Count -eq 0) {
        if ($AllowMissing) {
            return $null
        }
        throw "No fat jar matching '$ExpectedPrefix*-fat.jar' was found in target/. Run a package build first or pass -Rebuild."
    }

    $jarList = ($fatJars | Select-Object -ExpandProperty Name) -join ', '
    throw "Multiple fat jars were found in target/: $jarList. Clean target/ or rebuild to leave a single packaging artifact."
}

$fatJar = $null
if (-not $Rebuild) {
    $fatJar = Resolve-FatJar -Directory $targetDir -ExpectedPrefix "$artifactId-$projectVersion" -AllowMissing
}

if ($Rebuild -or -not $fatJar) {
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

    $fatJar = Resolve-FatJar -Directory $targetDir -ExpectedPrefix "$artifactId-$projectVersion"
}

$jpackage = Join-Path $jdkHome 'bin/jpackage.exe'
if (-not (Test-Path $jpackage)) {
    throw "jpackage.exe was not found in the selected JDK."
}

$distDir = Join-Path $projectRoot 'dist'
$tempRoot = Join-Path $projectRoot '.jpackage-temp'
$stagingRoot = Join-Path $tempRoot 'input'
$packageTemp = Join-Path $tempRoot $Type
$packageInput = Join-Path $stagingRoot $Type
$outputPath = if ($Type -eq 'app-image') {
    Join-Path $distDir 'DataLens'
} else {
    Join-Path $distDir ("DataLens-$AppVersion.$Type")
}
New-Item -ItemType Directory -Force -Path $distDir,$tempRoot,$stagingRoot | Out-Null
if (Test-Path $packageTemp) {
    Remove-Item -Recurse -Force $packageTemp
}
if (Test-Path $packageInput) {
    Remove-Item -Recurse -Force $packageInput
}
if (Test-Path $outputPath) {
    Remove-Item -Recurse -Force $outputPath
}
New-Item -ItemType Directory -Force -Path $packageTemp,$packageInput | Out-Null

# Stage only the runtime artifact needed by jpackage to avoid bundling test
# reports, test classes, and other machine-specific build metadata from target/.
Copy-Item -Path $fatJar.FullName -Destination (Join-Path $packageInput $fatJar.Name) -Force

$args = @(
    '--type', $Type,
    '--name', 'DataLens',
    '--dest', $distDir,
    '--temp', $packageTemp,
    '--input', $packageInput,
    '--main-jar', $fatJar.Name,
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
