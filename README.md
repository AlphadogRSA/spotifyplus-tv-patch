# Spotify+ TV 1.3.0.2

A personal modification of the inspected Spotify TV 1.134.2 ARMv7 client. **One installed Spotify app**, with the custom controls integrated into Spotify’s existing Settings/profile menu.

## Download the latest build

**[Latest release: Spotify+ TV 1.3.0.2](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/latest)** · Built 27 October 2026

- [Download the signed Android TV APK](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.2/SpotifyPlus-TV-1.3.0.2.apk)
- [Download the Windows installer ZIP](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.2/SpotifyPlus-TV-1.3.0.2-Windows-Installer.zip)
- [SHA-256 checksums](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.2/SHA256SUMS.txt)

This is an unofficial build for the inspected **ARMv7 Android TV** installation of Spotify TV 1.134.2 (`com.spotify.tv.android`). The APK combines the ARMv7, English and TV-density splits into one installable file; it is not universal across TV architectures. The Windows package installs the app on your TV through ADB; it is not a Windows Spotify app.

## Install or update from Windows

1. Download and extract the entire Windows installer ZIP.
2. Enable debugging on the TV and connect the PC and TV to the same network.
3. Double-click **Install-Spotify.cmd**. Enter the TV IP address when prompted and accept the TV debugging authorization.
4. The installer checks the APK checksum, updates Spotify, grants the audio diagnostic permission, launches the app and starts the embedded Hush helper.

The package includes the signed APK and official Android platform-tools. No Java installation, private signing key or administrator access is required. Updates use `adb install -r` without uninstalling Spotify or clearing app data. Updating an existing installation requires a matching signing certificate; if Android rejects the update, keep the existing app and inspect the error before taking further action.

For manual installation with an authorized ADB connection:

```powershell
adb -s TV_IP:5555 install -r SpotifyPlus-TV-1.3.0.2.apk
adb -s TV_IP:5555 shell pm grant com.spotify.tv.android android.permission.DUMP
adb -s TV_IP:5555 shell am start -W -n com.spotify.tv.android/.SpotifyTVActivity
```

Run `start-hush.ps1` from the extracted installer with `-Serial TV_IP:5555 -Adb .\tools\platform-tools\adb.exe` to start the helper after manual installation or a TV reboot. Hush is opt-in; enable it in Spotify's settings.

## Features in 1.3.0.2

- **Native settings:** appearance, artwork blur, motion, promotion hiding, player-local ad muting, Hush, EQ, lyrics and sleep timer. The existing Spotify rows stay in place. Menu/F1 or hold OK opens Spotify’s own Settings screen.
- **Graphic EQ:** dark rounded panel, blue vertical controls and connected curve inspired by the supplied reference. Ten frequency points from 31 Hz to 16 kHz map logarithmically onto the output’s available hardware bands. EQ stays ±6 dB; post-gain stays 0–6 dB. Existing five-band settings migrate once. Left/Right selects, Up/Down adjusts, OK moves to post-gain and then toolbar, Back closes.
- **Integrated Hush:** the helper code ships inside Spotify’s APK. An authorized ADB setup starts a shell-owned helper process, which can survive Spotify’s force-stop and relaunch it. No companion app or Shizuku installation is required. Hush is opt-in; helper setup is required after a TV reboot or APK update. See [HUSH.md](https://github.com/AlphadogRSA/spotifyplus-tv-patch/blob/v1.3.0.2/HUSH.md).
- **Existing features:** Spotify-only audio effects, local/LRCLIB lyrics, Aurora/Midnight/Ember appearance, artwork blur, motion, promotion hiding and sleep timer. The timer survives Spotify process restarts and clears on a TV reboot.

## Build from source

The rebuilt **single APK** is labeled `1.3.0.2`. It retains Android versionCode `2340200` from the inspected client. This is a personal extension version, not an upstream Spotify release. The build merges the inspected ARMv7/English/TV-density splits into one file; it is not an all-architecture universal package.

The source matching this release is available at **[tag v1.3.0.2](https://github.com/AlphadogRSA/spotifyplus-tv-patch/tree/v1.3.0.2)** and on `feat/hush-tv`. The default branch has not yet merged the feature implementation. To obtain the matching source and pinned helper:

```powershell
git clone --branch v1.3.0.2 --recurse-submodules https://github.com/AlphadogRSA/spotifyplus-tv-patch.git
```

`rebuild_release.py` verifies the exact previous v0.5 installation archive, replaces only the extension definitions/assets, adds the guarded embedded helper provider, updates the version name and merges the split resources/libraries. It produces an **unsigned APK**, which must be signed with the existing retained key to update your installed app. The original key is not stored here. Use `sign-apk.ps1`; do not uninstall or clear data to work around a mismatched key.

The local build passed APK v1/v2/v3 signature verification, prior-certificate matching, alignment, package/version checks, runtime/helper tests, Windows PowerShell payload validation, bundled ADB execution and archive/hash verification. The separate Gradle fingerprint check could not start locally because Java could not establish a loopback connection. Actual TV installation, D-pad behavior, Hush behavior, recovery/queue behavior and audio output still need device verification for this release. Earlier 0.5 device observations are documented separately in [VERIFICATION.md](VERIFICATION.md).

The original four-APK source build is also supported by `build.ps1` with your own original inputs and key. `install.ps1` installs that set, grants the existing audio diagnostic permission, launches Spotify and starts the embedded helper. For a signed single APK use `adb install -r`, grant `android.permission.DUMP`, launch Spotify, then run `start-hush.ps1`.

The Windows release ZIP includes the signed APK and official ADB platform-tools. Extract it and run `Install-Spotify.cmd`; Java and a local signing key are not required. It verifies the APK checksum and updates without uninstalling or clearing app data.

## Source and attribution

Original UI, lyrics and audio implementation is MIT licensed. The helper sources/adaptations from [coldMEW/Hush](https://github.com/coldMEW/Hush), pinned to `4e9ded397338de5c69b1784c636feb3d34a8de01`, are GPL-3.0 and embedded with their full license/source notice. The ReVanced build host retains its GPL license. Clone with `--recurse-submodules` to obtain the pinned helper source. None of these licenses relicense Spotify’s proprietary code, assets or recordings.

The build does not edit Spotify credentials, subscriptions or account entitlements. The local volume fallback affects only Spotify’s player; the graphic EQ attaches only to positive audio sessions owned by Spotify. There is no shared-output effect fallback. Optional manual LRCLIB lookup sends song title, artist, album and duration to that provider; see [LYRICS.md](LYRICS.md). Audio diagnostics remain in memory; see [AUDIO.md](AUDIO.md).
