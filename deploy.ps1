<#
.SYNOPSIS
    Local deployment pipeline for LearnHuayu.

.DESCRIPTION
    Runs the local pipeline from docs/06-pipeline.md: deploys the Cloudflare
    Worker API, builds the signed release APK, publishes it to the Google Drive
    apps folder, verifies the deployed file, and optionally commits the
    released source state. GitHub Actions (.github/workflows/ci.yml) remains
    the CI path; this script is the local release path.

.PARAMETER SkipWorker
    Skips the Cloudflare Worker deploy phase.

.PARAMETER SkipAndroid
    Skips the Android build and Google Drive deploy phase.

.PARAMETER DriveDir
    Google Drive destination folder. Defaults to G:\My Drive\myApps.

.PARAMETER Commit
    Commits the working tree after deployment. Requires -Message.

.PARAMETER Message
    Commit message used with -Commit.

.EXAMPLE
    .\deploy.ps1

.EXAMPLE
    .\deploy.ps1 -SkipWorker -Commit -Message "chore: release 0.1.0"
#>
[CmdletBinding()]
param(
    [switch]$SkipWorker,
    [switch]$SkipAndroid,
    [string]$DriveDir = "G:\My Drive\myApps",
    [switch]$Commit,
    [string]$Message
)

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
$apkName = "LearnHuayu.apk"
$driveApk = Join-Path $DriveDir $apkName
$startedAt = Get-Date

function Write-Step {
    param([string]$Text)
    Write-Host ""
    Write-Host "==> $Text" -ForegroundColor Cyan
}

function Assert-ExitCode {
    param([string]$Step)
    if ($LASTEXITCODE -ne 0) {
        throw "$Step failed with exit code $LASTEXITCODE."
    }
}

if ($SkipWorker -and $SkipAndroid -and -not $Commit) {
    throw "Nothing to deploy: both -SkipWorker and -SkipAndroid were passed without -Commit."
}

if (-not $SkipAndroid -and -not (Test-Path -LiteralPath $DriveDir -PathType Container)) {
    throw "Google Drive folder not found: $DriveDir. Start Google Drive for Desktop, or pass -DriveDir <path>."
}

if ($Commit -and [string]::IsNullOrWhiteSpace($Message)) {
    throw "-Commit requires -Message."
}

if (-not $SkipWorker) {
    Write-Step "Worker: syntax check, tests, and deploy (api/)"
    Push-Location (Join-Path $repoRoot "api")
    try {
        node --check worker.js
        Assert-ExitCode "node --check worker.js"
        npm test
        Assert-ExitCode "npm test"
        npm run deploy
        Assert-ExitCode "npm run deploy"
    }
    finally {
        Pop-Location
    }
}

if (-not $SkipAndroid) {
    Write-Step "Android: build release APK and deploy to Google Drive"
    Push-Location (Join-Path $repoRoot "android")
    try {
        & .\gradlew.bat :app:deployToDrive "-PdriveDir=$DriveDir" --console=plain
        Assert-ExitCode "gradlew.bat :app:deployToDrive"
    }
    finally {
        Pop-Location
    }

    Write-Step "Android: verify deployed APK"
    $apk = Get-Item -LiteralPath $driveApk
    if ($apk.Length -le 0) {
        throw "Deployed APK is empty: $driveApk"
    }
    if ($apk.LastWriteTime -lt $startedAt.AddMinutes(-5)) {
        throw "Deployed APK is stale (LastWriteTime $($apk.LastWriteTime)): $driveApk"
    }
    $hash = (Get-FileHash -LiteralPath $driveApk -Algorithm SHA256).Hash
    Write-Host ("    {0}" -f $apk.FullName)
    Write-Host ("    {0:N0} bytes, {1:yyyy-MM-dd HH:mm:ss}, SHA-256 {2}" -f $apk.Length, $apk.LastWriteTime, $hash)
}

if ($Commit) {
    Write-Step "Git: commit released source state"
    Push-Location $repoRoot
    try {
        git add -A
        Assert-ExitCode "git add -A"
        git commit -m $Message
        Assert-ExitCode "git commit"
        git log -1 --oneline
    }
    finally {
        Pop-Location
    }
}

Write-Step "Deploy pipeline complete"
