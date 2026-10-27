# Handoff

## Current source version: 0.6 (Hush TV integration; device verification pending)

The Hush feature is in the TV menu. A pinned upstream companion, reproducible TV adaptations and same-key build/install support are under `hush/`; see HUSH.md. Native session/ad signals use a signature-protected bridge. The companion keeps restart recovery alive in a foreground service; player-local muting remains the fallback. The existing 0.5 device observations below are historical and do not verify 0.6. Original proprietary APKs and the retained private signing key are not available in this workspace, so no new signed Spotify installation set or TV deployment is claimed.

## Previously device-tested release: 0.5

The 0.5 follow-up fixes themes against verified TV class selectors, expands promotion/Upgrade hiding and optimizes DOM scanning, playback callbacks, dialog cleanup and DSP writes. See VERIFICATION.md for the measured checks. It keeps all 0.4 functions and persisted settings. The remote theme cycle and Spotify-only audio activation were observed after installing 0.5.

The default build now uses modular Kotlin ReVanced patches and a precompiled MIT extension DEX. It adds default-on recognized-ad muting, Upgrade-button hiding, Spotify-style accents and background blur, polished TV labels, and per-process/session EQ plus 0–6 dB boost. There is no shared-output/session-0 effect path. See AUDIO.md for one-time ADB diagnostic permission setup. `install.ps1` installs all four APKs, grants that permission and launches the client. The same retained private key was used for updates; no uninstall was used for 0.4. The user paired Spotify again during testing.

The final four-APK build is specific to the inspected Spotify TV 1.134.2 ARMv7/English/TV-density installation. Source archives and the private GitHub release exclude the local signing key, original-install backup, account data, fetched lyrics and user screenshots. The release installation archive necessarily includes the patched proprietary app and required splits for this personal installation; the MIT license does not relicense those binaries.

This repository contains only the original TV extension and build/test tools. The proprietary original APKs, patched APK set, account data, signing key and fetched lyrics are intentionally excluded.

## Completed

- Version-pinned injection into the inspected Spotify TV 1.134.2 client.
- Native D-pad menu, three themes, cinematic motion toggle, own-session sleep timer and private local LRC import.
- Lyrics update 0.2: manual public LRCLIB lookup, timed and plain results, per-track in-memory cache, local-file precedence, font sizes and timing offset.
- Background lookup, bounded response size, request cooldown, connect/read timeouts and an overall watchdog.
- Reproducible four-APK build/signing script, original input hashes, deterministic tests and CI workflow.
- Independent source/attribution notices; no Eevee implementation or assets copied.

## Next device checks

The new **Lyrics · local / LRCLIB** panel was observed fetching and displaying timed lyrics for a real signed-in track. Continue testing provider coverage, track changes while fetching, local precedence, pause/seek/resume, timing offsets, long-running drift and D-pad scrolling of plain lyrics. See VERIFICATION.md for exactly which checks ran.

## Building and updating

Use the original four installation APKs from your own TV and the private signing key retained locally. `build.ps1` verifies their hashes and produces a fully signed four-APK set. Updating with the same key preserves the current app's data; a different key normally requires reinstalling and signing in again. Do not install just the base APK or mix certificates across splits.

The extension's added network request is only the explicit LRCLIB lookup; no Spotify login credentials or entitlement values are processed by it. The runtime and its tests remain MIT licensed; the ReVanced host module is GPL-3.0-only under revanced/COPYING. See README.md, LYRICS.md and assets/INSPIRATION.md for scope and notices.
