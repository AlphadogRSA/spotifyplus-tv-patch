param([string]$KeyStore, [string]$Serial, [switch]$SignOnly)
$ErrorActionPreference = 'Stop'
$expectedCert = '1ff3a55d2b53b7f232d47584eed11ed4445d0eba6c735212d071e62734fc57a1'
$apk = Join-Path $PSScriptRoot 'SpotifyPlus-TV-1.3.0.1-unsigned.apk'
if ((Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLower() -ne '202c36000fb1579e3020ffb82ff86155bc8fe0cab508da5b07c0088fdb66c1f5') { throw 'APK checksum mismatch. Download and extract the package again.' }
# Use the bundled portable runtime. No Java installation, PATH edits or admin rights needed.
$javaArchive = Join-Path $PSScriptRoot 'tools/java17-windows-x64.zip'
if (-not (Test-Path -LiteralPath $javaArchive)) { throw 'Portable Java archive is missing. Extract the entire new installer ZIP, not just the script.' }
if ((Get-FileHash -LiteralPath $javaArchive -Algorithm SHA256).Hash.ToLower() -ne '7fe2324cc5901a89aaa0e8dc232075c59f2aca5270c533bdb1b861a5274af834') { throw 'Portable Java checksum mismatch. Download and extract the new installer again.' }
$javaDirectory = Join-Path $PSScriptRoot 'tools/java17'
$java = Join-Path $javaDirectory 'jdk-17.0.20+8-jre/bin/java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    Write-Host 'Preparing bundled portable Java 17 (first run only)...'
    Expand-Archive -LiteralPath $javaArchive -DestinationPath $javaDirectory -Force
}
if (-not (Test-Path -LiteralPath $java)) { throw 'Portable Java could not be extracted. Make sure the folder is writable, then run again.' }
if (-not $KeyStore) {
    $candidates = @((Join-Path $env:USERPROFILE '.android/debug.keystore'))
    if ($env:ANDROID_USER_HOME) { $candidates += Join-Path $env:ANDROID_USER_HOME 'debug.keystore' }
    if ($env:ANDROID_SDK_HOME) { $candidates += Join-Path $env:ANDROID_SDK_HOME '.android/debug.keystore' }
    $KeyStore = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}
if (-not $KeyStore) { $KeyStore = Read-Host 'Path to the signing .keystore/.jks used for the previous TV build (blank to stop)' }
if (-not $KeyStore -or -not (Test-Path -LiteralPath $KeyStore)) {
    throw 'The original signing key was not found. Nothing was installed or uninstalled. Keep the existing TV app; share this message so we can choose the next step.'
}
$key = (Resolve-Path -LiteralPath $KeyStore).Path
$classPath = (Join-Path $PSScriptRoot 'tools/SpotifyTvSigner.jar') + ';' + (Join-Path $PSScriptRoot 'tools/apksig.jar')
$output = Join-Path $PSScriptRoot ('SpotifyPlus-TV-1.3.0.1-signed-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.apk')
Write-Host 'Checking the signing key against your previous TV build...'
& $java -cp $classPath SpotifyTvSigner $apk $key $output $expectedCert
if ($LASTEXITCODE -ne 0) { throw 'Signing stopped. The key may not match or its password may differ. Nothing was installed or uninstalled. Custom passwords can be supplied locally through SPTV_STORE_PASSWORD and SPTV_KEY_PASSWORD.' }
if ($SignOnly) { Write-Host "Installable same-key APK: $output"; exit 0 }
$adb = $null
$command = Get-Command adb -ErrorAction SilentlyContinue
if ($command) { $adb = $command.Source }
if (-not $adb -and $env:LOCALAPPDATA) {
    $candidate = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
    if (Test-Path -LiteralPath $candidate) { $adb = $candidate }
}
if (-not $adb) {
    $candidate = Join-Path $PSScriptRoot 'tools/platform-tools/adb.exe'
    if (-not (Test-Path -LiteralPath $candidate)) {
        Write-Host 'Downloading official Android platform-tools for the TV connection...'
        $archive = Join-Path $PSScriptRoot 'tools/platform-tools.zip'
        Invoke-WebRequest -UseBasicParsing -Uri 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $archive
        Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $PSScriptRoot 'tools') -Force
    }
    $adb = $candidate
}
if (-not $Serial) {
    $devices = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
    if ($devices.Count -eq 1) { $Serial = $devices[0] }
    elseif ($devices.Count -gt 1) { $Serial = Read-Host 'More than one ADB device is connected. Enter the TV serial/IP' }
    else {
        $Serial = Read-Host 'Enter the TV IP address (enable network/USB debugging on the TV first)'
        if ($Serial -notmatch '^(\d{1,3}\.){3}\d{1,3}(:\d{1,5})?$') { throw 'Enter an IPv4 address, optionally followed by :5555.' }
        if ($Serial -notmatch ':') { $Serial += ':5555' }
        & $adb connect $Serial
        Write-Host 'Accept the debugging authorization on the TV if prompted.'
    }
}
& $adb -s $Serial get-state
if ($LASTEXITCODE -ne 0) { throw "TV connection is not authorized/ready. The signed APK is saved at $output. Nothing was uninstalled." }
Write-Host 'Updating Spotify without uninstalling or clearing data...'
& $adb -s $Serial install -r $output
if ($LASTEXITCODE -ne 0) { throw 'Update failed. The existing app was not uninstalled or cleared. Keep the error shown above.' }
& $adb -s $Serial shell pm grant com.spotify.tv.android android.permission.DUMP
if ($LASTEXITCODE -ne 0) { Write-Warning 'Spotify installed, but EQ diagnostic permission needs setup.' }
& $adb -s $Serial shell am start -W -n com.spotify.tv.android/.SpotifyTVActivity
if ($LASTEXITCODE -ne 0) { throw 'Spotify installed, but could not launch. Open it on the TV manually.' }
try { & (Join-Path $PSScriptRoot 'start-hush.ps1') -Serial $Serial -Adb $adb }
catch { Write-Warning "Spotify installed; Hush helper setup is still pending: $($_.Exception.Message)" }
Write-Host 'Spotify update installed. Open Spotify Settings to check the custom settings, EQ and Hush helper status.'
