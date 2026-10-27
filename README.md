# Spotify+ TV 1.3.0.3

**An open-source Android TV extension and patch project**, maintained by [Tumelo Rathebe / AlphadogRSA](https://github.com/AlphadogRSA), for the inspected Spotify TV 1.134.2 ARMv7 client. **One installed Spotify app**, with the custom controls integrated into Spotify’s existing Settings/profile menu.

## Download the latest build

**[Latest release: Spotify+ TV 1.3.0.3](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/latest)** · Built 27 October 2026

- [Download the signed Android TV APK](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.3/SpotifyPlus-TV-1.3.0.3.apk)
- [Download the Windows installer ZIP](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.3/SpotifyPlus-TV-1.3.0.3-Windows-Installer.zip)
- [SHA-256 checksums](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.3/SHA256SUMS.txt)

This is an unofficial build for the inspected **ARMv7 Android TV** installation of Spotify TV 1.134.2 (`com.spotify.tv.android`). The APK combines the ARMv7, English and TV-density splits into one installable file; it is not universal across TV architectures. The Windows package installs the app on your TV through ADB; it is not a Windows Spotify app.

## Install or update from Windows

1. Download and extract the entire Windows installer ZIP.
2. Enable debugging on the TV and connect the PC and TV to the same network.
3. Double-click **Install-Spotify.cmd**. Enter the TV IP address when prompted and accept the TV debugging authorization.
4. The installer checks the APK checksum, updates Spotify, grants the audio diagnostic permission, launches the app and starts the embedded Hush helper.

The package includes the signed APK and official Android platform-tools. No Java installation, private signing key or administrator access is required. Updates use `adb install -r` without uninstalling Spotify or clearing app data. Updating an existing installation requires a matching signing certificate; if Android rejects the update, keep the existing app and inspect the error before taking further action.

For manual installation with an authorized ADB connection:

```powershell
adb -s TV_IP:5555 install -r SpotifyPlus-TV-1.3.0.3.apk
adb -s TV_IP:5555 shell pm grant com.spotify.tv.android android.permission.DUMP
adb -s TV_IP:5555 shell am start -W -n com.spotify.tv.android/.SpotifyTVActivity
```

Run `start-hush.ps1` from the extracted installer with `-Serial TV_IP:5555 -Adb .\tools\platform-tools\adb.exe` to start the helper after manual installation or a TV reboot. Hush is opt-in; enable it in Spotify's settings.

## Features in 1.3.0.3

- **Native settings:** appearance, artwork blur, motion, promotion hiding, player-local ad muting, Hush, EQ, lyrics and sleep timer. The existing Spotify rows stay in place. Menu/F1 or hold OK opens Spotify’s own Settings screen.
- **Graphic EQ:** dark rounded panel, blue vertical controls and connected curve inspired by the supplied reference. Ten frequency points from 31 Hz to 16 kHz map logarithmically onto the output’s available hardware bands. EQ stays ±6 dB; post-gain stays 0–6 dB. Existing five-band settings migrate once. Left/Right selects, Up/Down adjusts, OK moves to post-gain and then toolbar, Back closes.
- **Integrated Hush:** the helper code ships inside Spotify’s APK. An authorized ADB setup starts a shell-owned helper process, which can survive Spotify’s force-stop and relaunch it. No companion app or Shizuku installation is required. Hush is opt-in; helper setup is required after a TV reboot or APK update. See [HUSH.md](https://github.com/AlphadogRSA/spotifyplus-tv-patch/blob/v1.3.0.3/HUSH.md).
- **Existing features:** Spotify-only audio effects, local/LRCLIB lyrics, Aurora/Midnight/Ember appearance, artwork blur, motion, promotion hiding and sleep timer. The timer survives Spotify process restarts and clears on a TV reboot.

## Remote-navigation bugfix in 1.3.0.3

Fixed the issue where the new Spotify+ TV settings appeared but could not be reached with the TV remote. Spotify handles D-pad input through its native bridge, so ordinary browser keyboard listeners did not receive those events.

- Routes native Up/Down and OK events to the settings focus controller on the profile page.
- Moves between Spotify's original controls and all 11 custom rows, with scrolling to keep the selected row visible.
- Leaves native dialog controls and navigation outside the profile page with Spotify.
- Corrects the version displayed in About and the Ember settings-accent color.
- Adds regression coverage for virtual focus, native key routing, key-up handling and trusted-page boundaries.

**Verified on the Xiaomi TV:** installed 1.3.0.3 as a same-key update, reached all 11 rows using real ADB D-pad key events, and opened the native graphic EQ panel with OK. Existing app data and other installed apps were preserved. Sound quality and Hush ad-recovery behavior still need separate playback testing.

## Earlier fixes and improvements retained

Version 1.3.0.2 introduced the signed single-APK Windows package for the native-settings, EQ and Hush work in 1.3.0.1. Version 1.3.0.3 retains these improvements.

- **Settings available on first navigation:** registers the native settings bridge before Spotify loads its TV page, with trusted-page checks when an action runs.
- **Hush recovery state:** corrects the pending-recovery latch and retains cooldown, helper-readiness, pause and sleep-timer guards to limit repeated restart attempts.
- **Persistent sleep timer:** retains the deadline across Spotify process restarts and clears it after a TV reboot.
- **EQ migration and hardware mapping:** migrates the older five-band preferences and maps the ten displayed frequency controls onto the available hardware bands, with cached writes for unchanged settings.
- **Signing script repaired:** passes the configured signing arguments to `apksigner`, allowing same-key signing and verification to complete.
- **Simpler Windows updates:** ships the signed APK and ADB together, removes the Java/local-keystore requirement, and checks the APK hash using a Windows PowerShell-compatible implementation. Installation does not uninstall the app or reset its data.
- **Earlier theme and performance fixes retained:** broader TV focus/action/progress theme coverage, promotion filtering that preserves unrelated playback controls, fewer repeated DOM scans and audio-effect writes, and cleanup of closed dialogs.

These describe the implemented changes and automated checks. Installation and remote navigation were verified for 1.3.0.3; audio and Hush behavior still need playback verification.

## Build from source

The rebuilt **single APK** is labeled `1.3.0.3`. It retains Android versionCode `2340200` from the inspected client. This is a personal extension version, not an upstream Spotify release. The build merges the inspected ARMv7/English/TV-density splits into one file; it is not an all-architecture universal package.

The source matching this release is available at **[tag v1.3.0.3](https://github.com/AlphadogRSA/spotifyplus-tv-patch/tree/v1.3.0.3)** and on `feat/hush-tv`. The default branch has not yet merged the feature implementation. To obtain the matching source and pinned helper:

```powershell
git clone --branch v1.3.0.3 --recurse-submodules https://github.com/AlphadogRSA/spotifyplus-tv-patch.git
```

`rebuild_release.py` verifies the exact previous v0.5 installation archive, replaces only the extension definitions/assets, adds the guarded embedded helper provider, updates the version name and merges the split resources/libraries. It produces an **unsigned APK**, which must be signed with the existing retained key to update your installed app. The original key is not stored here. Use `sign-apk.ps1`; do not uninstall or clear data to work around a mismatched key.

The local build passed APK v1/v2/v3 signature verification, prior-certificate matching, alignment, package/version checks, runtime/helper tests, Windows PowerShell payload validation, bundled ADB execution and archive/hash verification. The separate Gradle fingerprint check could not start locally because Java could not establish a loopback connection. TV installation and D-pad navigation were verified for 1.3.0.3; Hush recovery/queue behavior and audio output still need device verification. Earlier 0.5 device observations are documented separately in [VERIFICATION.md](VERIFICATION.md).

The original four-APK source build is also supported by `build.ps1` with your own original inputs and key. `install.ps1` installs that set, grants the existing audio diagnostic permission, launches Spotify and starts the embedded helper. For a signed single APK use `adb install -r`, grant `android.permission.DUMP`, launch Spotify, then run `start-hush.ps1`.

The Windows release ZIP includes the signed APK and official ADB platform-tools. Extract it and run `Install-Spotify.cmd`; Java and a local signing key are not required. It verifies the APK checksum and updates without uninstalling or clearing app data.

## Open source and licenses

The extension, patch/build tooling, tests and helper adaptations are open source. You can inspect the [source for this release](https://github.com/AlphadogRSA/spotifyplus-tv-patch/tree/v1.3.0.3), report issues, and contribute changes under the applicable licenses.

- **Original TV UI, lyrics and audio implementation:** [MIT License](LICENSE).
- **Embedded Hush helper and adaptations:** GPL-3.0, with the [upstream source pinned in this release](https://github.com/AlphadogRSA/spotifyplus-tv-patch/tree/v1.3.0.3/hush) and [full license notice](https://github.com/AlphadogRSA/spotifyplus-tv-patch/blob/v1.3.0.3/assets/HUSH-COPYING.txt).
- **Kotlin ReVanced host module:** [GPL-3.0-only](revanced/COPYING).

Spotify's original application binaries, branding, artwork, music and recordings remain proprietary or owned by their respective rightsholders. The open-source licenses above apply to the relevant project code; they do not make the entire bundled Spotify APK open source or grant rights to third-party content.

## Credits and acknowledgements

Credit belongs to the developers and communities behind each respective project. Referencing them here acknowledges their work; it does not imply that they developed, endorsed or support this TV build.

| Developer / project | Contribution or inspiration |
| --- | --- |
| [Tumelo Rathebe / AlphadogRSA](https://github.com/AlphadogRSA) | Maintains this Android TV project, TV integration, builds and installer. |
| [Devon Shoutz / LeNerd46 — SpotifyPlus](https://github.com/LeNerd46/SpotifyPlus) | Inspiration for themes, motion, sleep timer and custom lyrics. The upstream MIT notice is retained; mobile hooks, fonts and assets are not copied. |
| [coldMEW — Hush](https://github.com/coldMEW/Hush) | Eight helper source files adapted from Hush 2.0.2, pinned at `4e9ded397338de5c69b1784c636feb3d34a8de01`, are embedded under GPL-3.0. |
| [SideloadLabs — EeveeSpotifyReincarnated](https://github.com/SideloadLabs/EeveeSpotifyReincarnated) | Inspiration for optional lyrics-provider and presentation features. No Eevee code, binaries, assets or iOS hooks are incorporated. |
| [Spicetify contributors](https://github.com/spicetify/cli) | Inspiration for CSS-based presentation customization. No Spicetify source, extensions or assets are bundled. |
| [ReVanced developers](https://github.com/ReVanced/revanced-patcher) | Official patching framework used by the Kotlin build host, with pinned CLI tooling. |
| [LRCLIB contributors](https://lrclib.net/docs) | Public API used by the independently implemented, optional manual lyrics lookup. Lyrics remain subject to rightsholders' rights and provider availability. |
| [Apktool / iBotPeaches contributors](https://github.com/iBotPeaches/Apktool) and [REAndroid / APKEditor contributors](https://github.com/REAndroid/APKEditor) | APK decoding/rebuilding and split-merging tools used in the release build. |
| [Android Open Source Project](https://source.android.com/) | Android SDK signing/alignment tools and official ADB platform-tools; bundled tool notices are retained. |

See the [inspiration notes](assets/INSPIRATION.md), [SpotifyPlus MIT notice](assets/SPOTIFYPLUS-LICENSE.txt) and [Hush source notice](https://github.com/AlphadogRSA/spotifyplus-tv-patch/blob/v1.3.0.3/assets/HUSH-NOTICE.txt) for attribution details.

The build does not edit Spotify credentials, subscriptions or account entitlements. The local volume fallback affects only Spotify’s player; the graphic EQ attaches only to positive audio sessions owned by Spotify. There is no shared-output effect fallback. Optional manual LRCLIB lookup sends song title, artist, album and duration to that provider; see [LYRICS.md](LYRICS.md). Audio diagnostics remain in memory; see [AUDIO.md](AUDIO.md).
