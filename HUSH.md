# Hush · Spotify+ TV

Version 0.6 adds **Hush · ad-break skipping** to the TV remote menu. It opens an optional companion adapted from [coldMEW/Hush 2.0.2](https://github.com/coldMEW/Hush), pinned to `4e9ded397338de5c69b1784c636feb3d34a8de01`.

Hush attempts to drop an ad break by restarting Spotify and resuming its new media session. Upstream measurements are for the phone client; they do not establish that Spotify TV will drop ads, keep the queue, or recover in the same time. This TV integration needs testing on the actual TV. Keep **Mute detected ads** on for player-local muting when restarting is unavailable or fails. A failed automatic attempt imposes a two-minute cooldown, with one attempt per break until real music resumes.

## Build and install

1. Clone this repository with `git clone --recurse-submodules`, or run `git submodule update --init --recursive` in an existing checkout.
2. Run the existing `build.ps1` with the original four Spotify TV 1.134.2 APKs, Android SDK 35/build-tools 35.0.0, a JDK 17 installation, and the same retained signing key used for your existing TV installation. Gradle and Maven dependency access is required. The build now also creates `signed/Hush-TV.apk`. Set `SPTV_STORE_PASSWORD` and, if needed, `SPTV_KEY_PASSWORD` outside the repository. `-WithoutHush` builds only the four Spotify splits.
3. Run `install.ps1 -ApkDirectory <signed-directory> -Serial <adb-device>`. This installs the companion first and then updates all four Spotify splits. There is no uninstall or data reset. Use `-WithoutHush` for a TV-only update.
4. In Spotify+ TV, press Menu or hold OK, choose **Hush · ad-break skipping**, then enable Hush.
5. Set up Hush's own helper using Wireless debugging on Android 11+, or authorize an already-running Shizuku on Android 8+. The original setup screens remain available; some TV firmware lacks the required Wireless debugging/pairing UI. On those TVs use Shizuku started with its documented computer/ADB setup. Helper availability after a reboot must be checked again.
6. Return to Spotify and start playback. The status screen should report a connected session. **Test Spotify restart** asks before interrupting playback. Check that the correct playlist resumes. Then test with a real ad.

The GitHub Actions `Hush-TV-unsigned` artifact is an unsigned companion build. It must be signed using your TV key before installation. It is not an installable update to the proprietary Spotify APK set. No original APKs or signing keys are stored in this repository.

## What crosses between the apps

The extension sends its own media-session token, native `is_ad_playing` flag, monotonic timestamp and sleep deadline to an explicit receiver. Both directions check the matching signing identity; the receiver also requires a signature permission. Old signals are rejected. No credentials, account entitlements or arbitrary shell commands are transmitted. The companion uses its native-session capability instead of requiring notification access on TV.

Hush is off by default. Enable/disable it in its TV screen. The companion never uses the upstream phone-wide ad-mute path: muting remains inside Spotify's player. Resume kicks target only Spotify's forwarded media session. The TV sleep deadline survives a Spotify process restart and is cleared after a TV reboot. If it expires during recovery, Hush stops attempting to resume playback.

DNS filtering is not required and stays off by default. The upstream optional VPN implementation remains in the separate companion; it is not activated by the TV feature or build/install scripts.

## Source and license

The upstream `hush/upstream` gitlink is unmodified. `hush/prepare.py` verifies that exact commit and clean tracked source, copies only tracked Android files into a new build directory, retargets Spotify's package, replaces the global-mute watcher path, connects native TV sessions, and adds `hush/overlay`. All adaptation is visible in these tracked files. Generated source is under `hush/build/android` and ignored by git.

Hush and the companion adaptations are GPL-3.0. The companion embeds the full upstream license and a source notice. The independent TV protocol adapter remains part of the MIT extension; no Hush implementation is linked into it. The ReVanced host retains its existing GPL license. None of these licenses relicense Spotify binaries.

## Device checks still required

- Pairing/helper setup and D-pad navigation on the actual firmware.
- New-session delivery and playback recovery after a test restart and a real ad.
- Playlist/queue behavior, ad-return failure, and the two-minute failure cooldown.
- Local mute restoration and whether EQ reattaches to the new Spotify audio session.
- Sleep expiry before/during restart, a background Spotify session, helper loss, and reboot recovery.
- Same-key updates preserve existing pairing, settings, lyrics and app data.
