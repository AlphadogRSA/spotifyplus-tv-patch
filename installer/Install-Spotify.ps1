param([string]$Serial, [switch]$ValidateOnly)
$ErrorActionPreference = 'Stop'
$apk = Join-Path $PSScriptRoot 'SpotifyPlus-TV-1.3.0.5.apk'
$expectedHash = '5b5a27978f8a3603ab772cef97d12bb1f74acf18ff511d058158850746fb18cb'
$hasher = [Security.Cryptography.SHA256]::Create()
try { $actualHash = [BitConverter]::ToString($hasher.ComputeHash([IO.File]::ReadAllBytes($apk))).Replace('-', '').ToLowerInvariant() } finally { $hasher.Dispose() }
if ($actualHash -ne $expectedHash) { throw 'APK checksum mismatch. Extract the installer ZIP again.' }
$output = $apk
if ($ValidateOnly) { Write-Host "Verified installer payload: $apk"; exit 0 }
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
