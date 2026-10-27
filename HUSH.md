# Integrated Hush · one Spotify app

Hush’s helper ships inside Spotify+ TV 1.3.0.1. There is no companion APK, extra launcher icon, notification-listener permission or Shizuku dependency. The shell-owned helper survives the Spotify process being stopped; Spotify’s next process resumes its own session. This adapts the restart strategy from coldMEW/Hush to the TV app’s native `is_ad_playing` signal.

## Setup

1. Build/sign with the same retained private key as the existing TV installation, then update without uninstalling. The generated unsigned APK cannot be installed until signed.
2. Launch Spotify once. The app writes a private, token-bound startup script for its embedded helper.
3. Run `start-hush.ps1 -Serial TV_IP:5555` over your authorized ADB connection. The command stays private; the script does not print its token. Repeat after a TV reboot or APK update. The app remains debuggable as in the existing build so the installer can read this app-private startup command through `run-as`.
4. Open Spotify’s own Settings/profile screen. Under Spotify+ TV, check **Hush helper setup**, then turn **Hush ad-break skipping** on. Keep **Mute detected ads** on for the fallback.

The helper provider accepts only Android’s shell UID, requires the privileged cross-user permission and validates a random token from private app storage. Its adapted command builder accepts only the fixed restart action for `com.spotify.tv.android/.SpotifyTVActivity`; it rejects arbitrary actions/components. It does not install or control another application.

## Recovery and limits

Before requesting a restart, Spotify commits its recovery state. The helper executes the whole stop/allowlist/start sequence outside the dying app process. The new process waits for its own media session, retries play for at most 20 seconds, honors the sleep deadline even if the visible timer has already cleared, and falls back after failure. Attempts have a 30-second minimum interval; failed recovery imposes a two-minute cooldown. Paused ads are left alone.

Whether restarting actually drops an ad break or preserves the queue depends on Spotify TV. Upstream phone timings do not establish TV behavior. This release needs real-device tests for ad-return behavior, queue context, helper loss, screen/background behavior, sleep expiry during restart, EQ reattachment and same-key updating. No calibrated audio or guaranteed ad-removal claim is made.

## Reproducible source

`hush/upstream` is an unchanged pinned submodule. `hush/prepare.py` verifies its commit and clean tracked source, then extracts/adapts eight helper classes under `dev.aurora.tvpatch.hush` into a new generated source directory. The command restriction, TV target, provider authority and protocol version changes are visible in that script. These helper portions are GPL-3.0; their full license and source notice are included in the APK. No phone UI, VPN/DNS filtering, notification watcher, global-volume mute or account handling is imported.
