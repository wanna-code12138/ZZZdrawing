$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$siblingToolchain = 'D:\soft\Opencode\JM Android\.toolchain'

function Test-AndroidJdk([string] $candidate) {
    if (-not $candidate) { return $false }
    foreach ($tool in @('java', 'javac', 'jlink', 'jmod')) {
        if (-not (Test-Path -LiteralPath (Join-Path $candidate "bin\$tool.exe"))) { return $false }
    }
    return $true
}

$localJdk = Join-Path $siblingToolchain 'jdk\jdk-21.0.12.1+1'
if (-not (Test-AndroidJdk $env:JAVA_HOME)) {
    if (Test-AndroidJdk $localJdk) {
        $env:JAVA_HOME = $localJdk
    } else {
        throw "Set JAVA_HOME to a full JDK 17+ with javac, jlink and jmod, or ensure $localJdk exists."
    }
}

$localSdk = Join-Path $siblingToolchain 'android-sdk'
if (-not $env:ANDROID_HOME -or -not (Test-Path -LiteralPath $env:ANDROID_HOME)) {
    if (Test-Path -LiteralPath $localSdk) {
        $env:ANDROID_HOME = $localSdk
    } else {
        throw "ANDROID_HOME not found and $localSdk does not exist."
    }
}

$env:ANDROID_USER_HOME = Join-Path $siblingToolchain 'android-user'
$env:GRADLE_USER_HOME = Join-Path $siblingToolchain 'gradle-home'
foreach ($directory in @($env:ANDROID_USER_HOME, $env:GRADLE_USER_HOME)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}

$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + (Join-Path $env:ANDROID_HOME 'platform-tools') + ';' + $env:PATH
