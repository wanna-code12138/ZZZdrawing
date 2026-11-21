param([Parameter(ValueFromRemainingArguments = $true)][string[]] $GradleArgs)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'android-env.ps1')

$buildArguments = @('--console=plain')
$internetSettings = Get-ItemProperty -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -ErrorAction SilentlyContinue
if ($internetSettings.ProxyEnable -eq 1 -and $internetSettings.ProxyServer) {
    $proxySettings = @{}
    $definition = [string] $internetSettings.ProxyServer
    if ($definition.Contains('=')) {
        foreach ($part in $definition.Split(';')) {
            $pair = $part.Split('=', 2)
            if ($pair.Length -eq 2) { $proxySettings[$pair[0]] = $pair[1] }
        }
    } else {
        $proxySettings['http'] = $definition
        $proxySettings['https'] = $definition
    }
    foreach ($scheme in @('http', 'https')) {
        if ($proxySettings[$scheme]) {
            $address = [string] $proxySettings[$scheme]
            if (-not $address.Contains('://')) { $address = 'http://' + $address }
            $proxyUri = [Uri] $address
            if ($proxyUri.UserInfo) { throw 'Authenticated proxy not supported by build script.' }
            $buildArguments += "-D$scheme.proxyHost=$($proxyUri.Host)"
            $buildArguments += "-D$scheme.proxyPort=$($proxyUri.Port)"
        }
    }
}

$gradle = 'D:\soft\Opencode\JM Android\.toolchain\gradle-9.6.0\bin\gradle.bat'
if (-not (Test-Path -LiteralPath $gradle)) {
    $gradle = Join-Path $projectRoot 'gradlew.bat'
}

Push-Location $projectRoot
try {
    & $gradle @buildArguments @GradleArgs
    $buildExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $buildExitCode
