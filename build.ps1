param(
    [Parameter(Mandatory)][string]$OriginalDirectory,
    [Parameter(Mandatory)][string]$AndroidSdk,
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$ApktoolJar,
    [Parameter(Mandatory)][string]$KeyStore,
    [Parameter(Mandatory)][string]$OutputDirectory,
    [string]$KeyAlias = 'androiddebugkey'
)
$ErrorActionPreference = 'Stop'
if (-not $env:SPTV_STORE_PASSWORD) { throw 'Set SPTV_STORE_PASSWORD for your own keystore; never commit the key or password.' }
if (Test-Path -LiteralPath $OutputDirectory) { throw 'OutputDirectory must be new so previous build results cannot be mixed.' }
$original = (Resolve-Path -LiteralPath $OriginalDirectory).Path
$sdk = (Resolve-Path -LiteralPath $AndroidSdk).Path
$jdk = (Resolve-Path -LiteralPath $JavaHome).Path
$apktool = (Resolve-Path -LiteralPath $ApktoolJar).Path
$signingKey = (Resolve-Path -LiteralPath $KeyStore).Path
$tools = Join-Path $sdk 'build-tools/35.0.0'
$androidJar = Join-Path $sdk 'platforms/android-35/android.jar'
$hashes = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'original_hashes.json') -Raw | ConvertFrom-Json
foreach ($entry in $hashes.PSObject.Properties) {
    $inputApk = Join-Path $original $entry.Name
    if ((Get-FileHash -LiteralPath $inputApk -Algorithm SHA256).Hash.ToLower() -ne $entry.Value) { throw "Wrong original APK: $($entry.Name)" }
}
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
$out = (Resolve-Path -LiteralPath $OutputDirectory).Path
foreach ($name in @('classes','dex','framework','aligned','signed')) { New-Item -ItemType Directory -Path (Join-Path $out $name) | Out-Null }
$env:JAVA_HOME = $jdk
function Check-Exit([string]$Stage) { if ($LASTEXITCODE -ne 0) { throw "$Stage failed ($LASTEXITCODE)" } }
$sources = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src/dev/aurora/tvpatch') -Filter '*.java' | ForEach-Object FullName
& (Join-Path $jdk 'bin/javac.exe') -encoding UTF-8 -source 8 -target 8 -cp $androidJar -d (Join-Path $out 'classes') @sources
Check-Exit 'Java compilation'
& (Join-Path $jdk 'bin/jar.exe') cf (Join-Path $out 'extension.jar') -C (Join-Path $out 'classes') .
Check-Exit 'Class packaging'
& (Join-Path $tools 'd8.bat') --min-api 26 --lib $androidJar --output (Join-Path $out 'dex') (Join-Path $out 'extension.jar')
Check-Exit 'D8'
python (Join-Path $PSScriptRoot 'patch.py') --base (Join-Path $original 'base.apk') --apktool $apktool --java (Join-Path $jdk 'bin/java.exe') --work (Join-Path $out 'patch') --dex (Join-Path $out 'dex/classes.dex') --framework (Join-Path $out 'framework')
Check-Exit 'Injection and APK rebuild'
foreach ($entry in $hashes.PSObject.Properties) {
    $inputApk = if ($entry.Name -eq 'base.apk') { Join-Path $out 'patch/patched-unsigned.apk' } else { Join-Path $original $entry.Name }
    $aligned = Join-Path $out "aligned/$($entry.Name)"
    $signed = Join-Path $out "signed/$($entry.Name)"
    & (Join-Path $tools 'zipalign.exe') -P 16 -f 4 $inputApk $aligned
    Check-Exit 'Alignment'
    $signArgs = @('sign','--ks',$signingKey,'--ks-key-alias',$KeyAlias,'--ks-pass','env:SPTV_STORE_PASSWORD','--out',$signed)
    if ($env:SPTV_KEY_PASSWORD) { $signArgs += @('--key-pass','env:SPTV_KEY_PASSWORD') }
    $signArgs += $aligned
    & (Join-Path $tools 'apksigner.bat') @signArgs
    Check-Exit 'Signing'
    & (Join-Path $tools 'apksigner.bat') verify --min-sdk-version 26 $signed
    Check-Exit 'Signature verification'
}
Compress-Archive -Path (Join-Path $out 'signed/*.apk') -DestinationPath (Join-Path $out 'SpotifyPlus-TV-patched.zip')
Write-Output "Verified four-APK installation set: $(Join-Path $out 'signed')"
