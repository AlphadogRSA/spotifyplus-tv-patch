param(
    [Parameter(Mandatory)][string]$Serial,
    [string]$Adb = 'adb'
)
$ErrorActionPreference = 'Stop'
# Spotify must have launched once to create its app-private, token-bound startup command.
$startup = $null
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    $content = & $Adb -s $Serial shell run-as com.spotify.tv.android cat files/sptv-hush-start.sh 2>$null
    if ($LASTEXITCODE -eq 0 -and $content) { $startup = ($content -join "`n").Trim(); break }
    Start-Sleep -Milliseconds 250
}
if (-not $startup) { throw 'Hush startup file is not ready. Open Spotify once, then run start-hush.ps1 again.' }
# Never echo the private helper token. Only accept the generated, fixed app_process entrypoint.
if ($startup -notmatch '^CLASSPATH=/data/app/[^\s;]+ setsid /system/bin/app_process /system/bin --nice-name=hush_helper dev\.aurora\.tvpatch\.hush\.Helper --token=[a-f0-9]{32} --uid=\d+ --apk=/data/app/[^\s;]+ --authority=com\.spotify\.tv\.android\.sptv_hush_helper </dev/null >/dev/null 2>&1 & sleep 1$') {
    throw 'Unexpected helper startup command; refusing to execute it.'
}
& $Adb -s $Serial shell $startup
if ($LASTEXITCODE -ne 0) { throw 'Hush helper startup failed. Local ad muting remains available.' }
$startup = $null
Write-Output 'Hush helper startup requested. Verify Helper ready in Spotify Settings before enabling restart skipping.'
