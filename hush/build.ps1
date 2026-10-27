param(
    [Parameter(Mandatory)][string]$AndroidSdk,
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$KeyStore,
    [Parameter(Mandatory)][string]$OutputApk,
    [string]$KeyAlias = 'androiddebugkey'
)
$ErrorActionPreference = 'Stop'
if (-not $env:SPTV_STORE_PASSWORD) { throw 'Set SPTV_STORE_PASSWORD for the same private key used by Spotify+ TV.' }
if (Test-Path -LiteralPath $OutputApk) { throw 'OutputApk must be new.' }
$sdk = (Resolve-Path -LiteralPath $AndroidSdk).Path
$jdk = (Resolve-Path -LiteralPath $JavaHome).Path
$key = (Resolve-Path -LiteralPath $KeyStore).Path
$tools = Join-Path $sdk 'build-tools/35.0.0'
$project = Join-Path $PSScriptRoot 'build/android'
function Check-Exit([string]$Stage) { if ($LASTEXITCODE -ne 0) { throw "$Stage failed ($LASTEXITCODE)" } }
$oldJava = $env:JAVA_HOME
$oldSdk = $env:ANDROID_HOME
$oldSigning = $env:HUSH_SIGNING
try {
    $env:JAVA_HOME = $jdk
    $env:ANDROID_HOME = $sdk
    # Keep upstream signing config out of this build; sign explicitly using the TV key below.
    $env:HUSH_SIGNING = Join-Path $PSScriptRoot 'build/no-upstream-signing.properties'
    if (Test-Path -LiteralPath $env:HUSH_SIGNING) { throw 'Unexpected upstream signing properties file; remove it first.' }
    git -C $PSScriptRoot submodule update --init --recursive -- upstream
    Check-Exit 'Pinned Hush checkout'
    # This is a generated build directory only, never the pinned source or signing material.
    if (Test-Path -LiteralPath $project) { Remove-Item -LiteralPath $project -Recurse -Force }
    python (Join-Path $PSScriptRoot 'prepare.py') --output $project
    Check-Exit 'TV adaptation'
    & (Join-Path $project 'gradlew.bat') -p $project testDebugUnitTest assembleRelease
    Check-Exit 'Hush tests and Android build'
    $unsigned = Join-Path $project 'app/build/outputs/apk/release/app-release-unsigned.apk'
    $aligned = Join-Path $PSScriptRoot 'build/Hush-TV-aligned.apk'
    & (Join-Path $tools 'zipalign.exe') -P 16 -f 4 $unsigned $aligned
    Check-Exit 'Hush alignment'
    $signArgs = @('sign','--ks',$key,'--ks-key-alias',$KeyAlias,'--ks-pass','env:SPTV_STORE_PASSWORD','--out',$OutputApk)
    if ($env:SPTV_KEY_PASSWORD) { $signArgs += @('--key-pass','env:SPTV_KEY_PASSWORD') }
    $signArgs += $aligned
    & (Join-Path $tools 'apksigner.bat') @signArgs
    Check-Exit 'Hush signing'
    & (Join-Path $tools 'apksigner.bat') verify --min-sdk-version 26 $OutputApk
    Check-Exit 'Hush signature verification'
} finally {
    $env:JAVA_HOME = $oldJava
    $env:ANDROID_HOME = $oldSdk
    $env:HUSH_SIGNING = $oldSigning
}
