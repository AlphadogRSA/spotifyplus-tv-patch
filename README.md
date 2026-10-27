# Spotify+ TV 1.3.0.9

**An open-source Android TV extension and patch project**, maintained by [Tumelo Rathebe / AlphadogRSA](https://github.com/AlphadogRSA), for the inspected Spotify TV 1.134.2 ARMv7 client. Custom controls live inside Spotify's existing Settings/profile menu.

## Download the latest build

**[Latest release: Spotify+ TV 1.3.0.9](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/latest)** · Built 27 October 2026

- [Signed Android TV APK](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.9/SpotifyPlus-TV-1.3.0.9.apk)
- [Windows installer ZIP](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.9/SpotifyPlus-TV-1.3.0.9-Windows-Installer.zip)
- [SHA-256 checksums](https://github.com/AlphadogRSA/spotifyplus-tv-patch/releases/download/v1.3.0.9/SHA256SUMS.txt)

This unofficial APK targets the inspected **ARMv7 Android TV** installation of Spotify TV 1.134.2 (`com.spotify.tv.android`). It merges ARMv7, English and TV-density splits into one file. The Windows package installs on a TV through ADB; it is not a Windows Spotify client.

## New and fixed in 1.3.0.9

- **Native Spotify menu appearance:** ten Spotify+ rows reuse Spotify's own list buttons, typography, subtitle and focus styles instead of custom cards. The EQ-style native dialogs remain for EQ, Sleep, Hush Setup and About.
- **Experimental lyrics retired:** the unstable experimental lyrics feature has been removed to simplify setup and maintenance. Its menu, provider network requests, parser, dialogs and polling are gone. Existing app data is preserved; old imported files are simply unused.
- **Faster input path:** Search/text input bypasses the extension's asynchronous key-routing and long-OK interception. A menu-to-Search press still receives its matching key release. Android keeps control of the keyboard and typing.
- **Less background work:** hidden settings skip state refreshes; focus mode is cached and reported only when it changes. Unrelated Home artwork/card mutations no longer rescan settings. Removing lyrics also removes its playback-metadata polling and worker.
- **Previous fixes retained:** bounded settings scrolling, Settings/Home focus handoff, visible-only navigation with Upgrade blocked, separate Home artwork blur halo, seven themes and the developer GitHub QR.

**TV verification:** installed as a same-key update with app data retained. Keyboard opening succeeded in three focused-Search trials at 3.12, 2.53 and 2.23 seconds, including ADB command and status-query overhead. These are device observations, not a controlled speed comparison or a claim that Android keyboard startup is instantaneous. Audio output, timer expiration and full Hush recovery still require separate playback testing. See [VERIFICATION.md](VERIFICATION.md).

## Current features

- Appearance: Spotify original, Aurora, Midnight, Ember, Ocean, Rose and Solar; artwork blur and cinematic motion.
- Spotify-only graphic EQ: ten frequency controls, connected curve, hardware-band mapping, +/-6 dB EQ and 0-6 dB post-gain. Existing settings migrate once.
- Opt-in player-local ad muting and integrated [Hush helper](https://github.com/AlphadogRSA/spotifyplus-tv-patch/blob/v1.3.0.9/HUSH.md). The embedded helper needs authorized ADB setup after an APK update or TV reboot.
- Sleep timer, retained across Spotify process restarts and cleared on TV reboot.
- About with developer credit and offline GitHub QR. Menu/F1 or hold OK outside text entry opens Spotify's Settings.

Upgrade's promotional UI is excluded from navigation, direct activation and its upsell route. This changes the interface without changing account subscriptions or streaming entitlements.

## Install or update from Windows

1. Download and extract the entire Windows installer ZIP.
2. Enable TV debugging and connect the PC and TV to the same network.
3. Double-click **Install-Spotify.cmd**, enter the TV IP address and accept the TV authorization prompt.
4. The installer checks the APK checksum, updates Spotify, grants the audio diagnostic permission, launches the app and starts its embedded helper.

The package contains the signed APK and official Android platform-tools. No Java, local signing key or administrator access is required. Updates use `adb install -r` without uninstalling or clearing app data. A certificate mismatch stops installation; keep the existing app and inspect that error.

```powershell
adb -s TV_IP:5555 install -r SpotifyPlus-TV-1.3.0.9.apk
adb -s TV_IP:5555 shell pm grant com.spotify.tv.android android.permission.DUMP
adb -s TV_IP:5555 shell am start -W -n com.spotify.tv.android/.SpotifyTVActivity
```

After manual installation or a TV reboot, run `start-hush.ps1 -Serial TV_IP:5555 -Adb .\tools\platform-tools\adb.exe` from the extracted installer. Hush remains opt-in.

## Build from source

The rebuilt **single APK** is labeled `1.3.0.9`. It retains Android versionCode `2340200` from the inspected client. This is a extension version, not an upstream Spotify release. The build merges the inspected ARMv7/English/TV-density splits into one file; it is not an all-architecture universal package.

The source matching this release is available at **[tag v1.3.0.9](https://github.com/AlphadogRSA/spotifyplus-tv-patch/tree/v1.3.0.9)** and on `feat/hush-tv`. The default branch has not yet merged the feature implementation. To obtain the matching source and pinned helper:

```powershell
git clone --branch v1.3.0.9 --recurse-submodules https://github.com/AlphadogRSA/spotifyplus-tv-patch.git
```

`rebuild_release.py` verifies the exact previous v0.5 installation archive, replaces only the extension definitions/assets, adds the guarded embedded helper provider, updates the version name and merges the split resources/libraries. It produces an **unsigned APK**, which must be signed with the existing retained key to update your installed app. The original key is not stored here. Use `sign-apk.ps1`; do not uninstall or clear data to work around a mismatched key.

The local build passed APK v1/v2/v3 signature verification, prior-certificate matching, alignment, package/version checks, runtime/helper tests, Windows PowerShell payload validation, bundled ADB execution and archive/hash verification. The separate Gradle fingerprint check could not start locally because Java could not establish a loopback connection. TV installation and D-pad navigation were verified for 1.3.0.9; Hush recovery/queue behavior and audio output still need device verification. Earlier 0.5 device observations are documented separately in [VERIFICATION.md](VERIFICATION.md).

The original four-APK source build is also supported by `build.ps1` with your own original inputs and key. `install.ps1` installs that set, grants the existing audio diagnostic permission, launches Spotify and starts the embedded helper. For a signed single APK use `adb install -r`, grant `android.permission.DUMP`, launch Spotify, then run `start-hush.ps1`.

The Windows release ZIP includes the signed APK and official ADB platform-tools. Extract it and run `Install-Spotify.cmd`; Java and a local signing key are not required. It verifies the APK checksum and updates without uninstalling or clearing app data.

## Open source and licenses

The extension, patch/build tooling, tests and helper adaptations are open source. You can inspect the [source for this release](https://github.com/AlphadogRSA/spotifyplus-tv-patch/tree/v1.3.0.9), report issues, and contribute changes under the applicable licenses.

- **Original TV UI and audio implementation:** [MIT License](LICENSE).
- **Embedded Hush helper and adaptations:** GPL-3.0, with the [upstream source pinned in this release](https://github.com/AlphadogRSA/spotifyplus-tv-patch/tree/v1.3.0.9/hush) and [full license notice](https://github.com/AlphadogRSA/spotifyplus-tv-patch/blob/v1.3.0.9/assets/HUSH-COPYING.txt).
- **Kotlin ReVanced host module:** [GPL-3.0-only](revanced/COPYING).

Spotify's original application binaries, branding, artwork, music and recordings remain proprietary or owned by their respective rightsholders. The open-source licenses above apply to the relevant project code; they do not make the entire bundled Spotify APK open source or grant rights to third-party content.

## Credits and acknowledgements

Credit belongs to the developers and communities behind each respective project. Referencing them here acknowledges their work; it does not imply that they developed, endorsed or support this TV build.

| Developer / project | Contribution or inspiration |
| --- | --- |
| [Tumelo Rathebe / AlphadogRSA](https://github.com/AlphadogRSA) | Maintains this Android TV project, TV integration, builds and installer. |
| [Devon Shoutz / LeNerd46 â€” SpotifyPlus](https://github.com/LeNerd46/SpotifyPlus) | Inspiration for themes, motion, sleep timer and custom lyrics. The upstream MIT notice is retained; mobile hooks, fonts and assets are not copied. |
| [coldMEW â€” Hush](https://github.com/coldMEW/Hush) | Eight helper source files adapted from Hush 2.0.2, pinned at `4e9ded397338de5c69b1784c636feb3d34a8de01`, are embedded under GPL-3.0. |
| [SideloadLabs â€” EeveeSpotifyReincarnated](https://github.com/SideloadLabs/EeveeSpotifyReincarnated) | Historical inspiration for the retired experimental lyrics feature and presentation controls. No Eevee code, binaries, assets or iOS hooks are incorporated. |
| [Spicetify contributors](https://github.com/spicetify/cli) | Inspiration for CSS-based presentation customization. No Spicetify source, extensions or assets are bundled. |
| [ReVanced developers](https://github.com/ReVanced/revanced-patcher) | Official patching framework used by the Kotlin build host, with pinned CLI tooling. |
| [LRCLIB contributors](https://lrclib.net/docs) | Public API used by the retired experimental lyrics lookup in earlier releases. |
| [Apktool / iBotPeaches contributors](https://github.com/iBotPeaches/Apktool) and [REAndroid / APKEditor contributors](https://github.com/REAndroid/APKEditor) | APK decoding/rebuilding and split-merging tools used in the release build. |
| [Android Open Source Project](https://source.android.com/) | Android SDK signing/alignment tools and official ADB platform-tools; bundled tool notices are retained. |

See the [inspiration notes](assets/INSPIRATION.md), [SpotifyPlus MIT notice](assets/SPOTIFYPLUS-LICENSE.txt) and [Hush source notice](https://github.com/AlphadogRSA/spotifyplus-tv-patch/blob/v1.3.0.9/assets/HUSH-NOTICE.txt) for attribution details.

The build does not edit Spotify credentials, subscriptions or account entitlements. The local volume fallback affects only Spotifyâ€™s player; the graphic EQ attaches only to positive audio sessions owned by Spotify. There is no shared-output effect fallback. The experimental lyrics integration has been removed; see [LYRICS.md](LYRICS.md). Audio diagnostics remain in memory; see [AUDIO.md](AUDIO.md).
