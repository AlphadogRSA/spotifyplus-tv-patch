param(
    [Parameter(Mandatory)][string]$UnsignedApk,
    [Parameter(Mandatory)][string]$AndroidSdk,
    [Parameter(Mandatory)][string]$KeyStore,
    [Parameter(Mandatory)][string]$OutputApk,
    [string]$KeyAlias = 'androiddebugkey'
)
$ErrorActionPreference = 'Stop'
if (-not $env:SPTV_STORE_PASSWORD) { throw 'Set SPTV_STORE_PASSWORD for your retained TV signing key.' }
if (Test-Path -LiteralPath $OutputApk) { throw 'OutputApk must be new.' }
$inputApk = (Resolve-Path -LiteralPath $UnsignedApk).Path
$key = (Resolve-Path -LiteralPath $KeyStore).Path
$tools = Join-Path (Resolve-Path -LiteralPath $AndroidSdk).Path 'build-tools/35.0.0'
$aligned = [IO.Path]::Combine([IO.Path]::GetTempPath(), [IO.Path]::GetRandomFileName() + '.apk')
try {
    & (Join-Path $tools 'zipalign.exe') -P 16 -f 4 $inputApk $aligned
    if ($LASTEXITCODE -ne 0) { throw 'APK alignment failed.' }
    $signArgs = @('sign','--ks',$key,'--ks-key-alias',$KeyAlias,'--ks-pass','env:SPTV_STORE_PASSWORD','--out',$OutputApk)
    if ($env:SPTV_KEY_PASSWORD) { $signArgs += @('--key-pass','env:SPTV_KEY_PASSWORD') }
    $signArgs += $aligned
    & (Join-Path $tools 'apksigner.bat') @signArgs
    if ($LASTEXITCODE -ne 0) { throw 'APK signing failed.' }
    & (Join-Path $tools 'apksigner.bat') verify --min-sdk-version 26 $OutputApk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
} finally { Remove-Item -LiteralPath $aligned -ErrorAction SilentlyContinue }
