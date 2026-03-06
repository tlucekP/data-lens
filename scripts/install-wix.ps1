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

$lightExe = Join-Path $wixRoot 'light.exe'
$lightRealExe = Join-Path $wixRoot 'light-real.exe'
if (Test-Path $lightRealExe) { Remove-Item -Force $lightRealExe }
Move-Item -Force $lightExe $lightRealExe

$wrapperSource = @"
using System;
using System.Diagnostics;
using System.Linq;

public static class Program
{
    public static int Main(string[] args)
    {
        try
        {
            var realLight = Environment.GetEnvironmentVariable("DATALENS_WIX_REAL_LIGHT");
            if (string.IsNullOrWhiteSpace(realLight))
            {
                realLight = System.IO.Path.Combine(AppContext.BaseDirectory, "light-real.exe");
            }

            if (!System.IO.File.Exists(realLight))
            {
                Console.Error.WriteLine("light wrapper: missing real light.exe at " + realLight);
                return 2;
            }

            var extra = Environment.GetEnvironmentVariable("DATALENS_WIX_LIGHT_EXTRA") ?? string.Empty;
            var allArgs = string.IsNullOrWhiteSpace(extra)
                ? args
                : extra.Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries).Concat(args).ToArray();

            var startInfo = new ProcessStartInfo
            {
                FileName = realLight,
                UseShellExecute = false,
                Arguments = string.Join(" ", allArgs.Select(Quote))
            };

            using (var process = Process.Start(startInfo))
            {
                if (process == null)
                {
                    Console.Error.WriteLine("light wrapper: failed to start " + realLight);
                    return 3;
                }

                process.WaitForExit();
                return process.ExitCode;
            }
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine("light wrapper: " + ex.GetType().FullName + ": " + ex.Message);
            Console.Error.WriteLine(ex.StackTrace);
            return 1;
        }
    }

    private static string Quote(string value)
    {
        if (string.IsNullOrEmpty(value))
        {
            return "\"\"";
        }

        if (!value.Any(ch => char.IsWhiteSpace(ch) || ch == '"'))
        {
            return value;
        }

        return "\"" + value.Replace("\"", "\\\"") + "\"";
    }
}
"@
Add-Type -TypeDefinition $wrapperSource -OutputAssembly $lightExe -OutputType ConsoleApplication

$required = @('candle.exe', 'light.exe', 'light-real.exe')
foreach ($tool in $required) {
    if (-not (Test-Path (Join-Path $wixRoot $tool))) {
        throw "WiX installation is incomplete. Missing $tool."
    }
}

Write-Host "WiX Toolset extracted to $wixRoot"
Write-Host "light.exe wrapper installed with validation suppression support"