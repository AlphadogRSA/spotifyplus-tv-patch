SPOTIFY+ TV 1.3.0.1 - WINDOWS x64 UPDATE INSTALLER (PORTABLE JAVA INCLUDED)

1. Extract this entire ZIP on the Windows PC used for your previous Spotify TV build.
2. Double-click Install-Spotify.cmd.
3. Keep the PC and TV on the same network. Accept the TV's debugging authorization if asked.

The installer checks the previous build's signing certificate. It looks for the standard
Android debug keystore on your PC, or asks for your original .jks/.keystore path.
It signs and verifies the APK locally before attempting an update. It does not upload,
print or include your private signing key. It never uninstalls Spotify or clears app data.

If the key does not match or cannot be found, the installer stops. Do not uninstall the
working TV app to bypass this. Send the displayed error so the next step can be chosen.

Portable Eclipse Temurin Java 17.0.20+8 is included and checksum-verified.
It runs from the installer folder; no system Java setup or administrator rights are needed.
ADB is used if already installed; otherwise official Google platform-tools are downloaded.
For a non-default keystore password, set SPTV_STORE_PASSWORD locally before running the
PowerShell script. SPTV_KEY_PASSWORD is only needed when the key password differs.
Do not send passwords in chat or commit them to GitHub.

Signing only, without connecting to the TV:
powershell -NoProfile -ExecutionPolicy Bypass -File .\Install-Spotify.ps1 -SignOnly

This APK is for the inspected ARMv7 Android TV build. Automated source/build tests passed;
real TV navigation, sound, Hush ad skipping/recovery and queue behavior still require testing.
Hush uses an embedded ADB-started helper; no second app is installed. Run start-hush.ps1
again after a TV reboot. Source and GPL/MIT notices are in the source repository:
https://github.com/AlphadogRSA/spotifyplus-tv-patch/pull/1

Included signing library: Android apksig 8.4.0, Apache License 2.0.
https://source.android.com/docs/setup/about/licenses

Bundled runtime: Eclipse Temurin OpenJDK 17.0.20+8 for Windows x64 (JRE).
Source/release: https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.20%2B8
The runtime archive contains its original licenses and notices (GPLv2 with Classpath Exception).
