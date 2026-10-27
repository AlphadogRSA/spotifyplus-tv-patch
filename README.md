# Spotify+ TV 1.3.0.1

A personal modification of the inspected Spotify TV 1.134.2 ARMv7 client. **One installed Spotify app**, with the custom controls integrated into Spotify’s existing Settings/profile menu.

- **Native settings:** appearance, artwork blur, motion, promotion hiding, player-local ad muting, Hush, EQ, lyrics and sleep timer. The existing Spotify rows stay in place. Menu/F1 or hold OK opens Spotify’s own Settings screen.
- **Graphic EQ:** dark rounded panel, blue vertical controls and connected curve inspired by the supplied reference. Ten frequency points from 31 Hz to 16 kHz map logarithmically onto the output’s available hardware bands. EQ stays ±6 dB; post-gain stays 0–6 dB. Existing five-band settings migrate once. Left/Right selects, Up/Down adjusts, OK moves to post-gain and then toolbar, Back closes.
- **Integrated Hush:** the helper code ships inside Spotify’s APK. An authorized ADB setup starts a shell-owned helper process, which can survive Spotify’s force-stop and relaunch it. No companion app or Shizuku installation is required. Hush is opt-in; helper setup is required after a TV reboot or APK update. See [HUSH.md](HUSH.md).
- **Existing features:** Spotify-only audio effects, local/LRCLIB lyrics, Aurora/Midnight/Ember appearance, artwork blur, motion, promotion hiding and sleep timer. The timer survives Spotify process restarts and clears on a TV reboot.

## Build and install

The rebuilt **single APK** is labeled `1.3.0.1`. It retains Android versionCode `2340200` from the inspected client. This is a personal extension version, not an upstream Spotify release. The build merges the inspected ARMv7/English/TV-density splits into one file; it is not an all-architecture universal package.

`rebuild_release.py` verifies the exact previous v0.5 installation archive, replaces only the extension definitions/assets, adds the guarded embedded helper provider, updates the version name and merges the split resources/libraries. It produces an **unsigned APK**, which must be signed with the existing retained key to update your installed app. The original key is not stored here. Use `sign-apk.ps1`; do not uninstall or clear data to work around a mismatched key.

GitHub Actions builds the unsigned APK and runs source/helper, native-settings, presentation, audio, lyrics and patch-fingerprint checks. Actual TV install, D-pad behavior, ad skipping, recovery/queue behavior and audio output still need device verification for this release. Earlier 0.5 device observations are documented separately in [VERIFICATION.md](VERIFICATION.md).

The original four-APK source build is also supported by `build.ps1` with your own original inputs and key. `install.ps1` installs that set, grants the existing audio diagnostic permission, launches Spotify and starts the embedded helper. For a signed single APK use `adb install -r`, grant `android.permission.DUMP`, launch Spotify, then run `start-hush.ps1`.

## Source and attribution

Original UI, lyrics and audio implementation is MIT licensed. The helper sources/adaptations from [coldMEW/Hush](https://github.com/coldMEW/Hush), pinned to `4e9ded397338de5c69b1784c636feb3d34a8de01`, are GPL-3.0 and embedded with their full license/source notice. The ReVanced build host retains its GPL license. Clone with `--recurse-submodules` to obtain the pinned helper source. None of these licenses relicense Spotify’s proprietary code, assets or recordings.

The build does not edit Spotify credentials, subscriptions or account entitlements. The local volume fallback affects only Spotify’s player; the graphic EQ attaches only to positive audio sessions owned by Spotify. There is no shared-output effect fallback. Optional manual LRCLIB lookup sends song title, artist, album and duration to that provider; see [LYRICS.md](LYRICS.md). Audio diagnostics remain in memory; see [AUDIO.md](AUDIO.md).
