param(
    [Parameter(Mandatory)][string]$ApkDirectory,
    [Parameter(Mandatory)][string]$Serial,
    [string]$Adb = 'adb',
    [switch]$WithoutHush
)
$ErrorActionPreference = 'Stop'
$apks = @('base.apk','split_config.armeabi_v7a.apk','split_config.en.apk','split_config.tvdpi.apk') | ForEach-Object {
    (Resolve-Path -LiteralPath (Join-Path $ApkDirectory $_)).Path
}
# Install the permission-defining companion first. An existing matching signing key is required.
if (-not $WithoutHush) {
    $hush = (Resolve-Path -LiteralPath (Join-Path $ApkDirectory 'Hush-TV.apk')).Path
    & $Adb -s $Serial install -r $hush
    if ($LASTEXITCODE -ne 0) { throw 'Hush installation failed; no uninstall or app data reset was attempted.' }
}
& $Adb -s $Serial install-multiple -r @apks
if ($LASTEXITCODE -ne 0) { throw 'Installation failed; no uninstall or app data reset was attempted.' }
# Development diagnostic permission: identifies only this process’s positive audio session IDs.
# It does not enable shared-output effects. The app parses diagnostics in memory only.
& $Adb -s $Serial shell pm grant com.spotify.tv.android android.permission.DUMP
if ($LASTEXITCODE -ne 0) { throw 'Installed, but audio-session discovery setup failed. See AUDIO.md.' }
& $Adb -s $Serial shell am force-stop com.spotify.tv.android
& $Adb -s $Serial shell am start -W -n com.spotify.tv.android/.SpotifyTVActivity
if ($LASTEXITCODE -ne 0) { throw 'Installed, but launch failed.' }
