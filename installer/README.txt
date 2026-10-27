SPOTIFY+ TV 1.3.0.4 - WINDOWS UPDATE INSTALLER - 27 OCTOBER 2026

Extract the entire ZIP, then double-click Install-Spotify.cmd.
Keep the PC and TV on the same network and enable TV debugging.
Accept the TV authorization prompt if asked. Enter the TV IPv4 address when prompted.

Includes the same-key signed APK and official Android platform-tools.
No Java, signing key, administrator access or system installation is required.
The installer checks the APK SHA256 before connecting. It updates using adb install -r,
grants the audio diagnostic permission, launches Spotify and starts the embedded Hush helper.
It never uninstalls Spotify or clears its data. A signature mismatch stops the update.

Validation without connecting or installing:
powershell -NoProfile -ExecutionPolicy Bypass -File .\Install-Spotify.ps1 -ValidateOnly

Hush is opt-in. Run start-hush.ps1 again after a TV reboot.
Target: the inspected Spotify TV 1.134.2 ARMv7 Android TV installation.
Version name: 1.3.0.4; original Android versionCode retained for same-key updates.
TV playback, D-pad, EQ sound and helper behavior require real-device testing.
Source: https://github.com/AlphadogRSA/spotifyplus-tv-patch/tree/feat/hush-tv
Original runtime: MIT; embedded Hush and patcher: GPL-3.0; Spotify remains proprietary.
Official platform-tools notices are included under tools/platform-tools/NOTICE.txt.
