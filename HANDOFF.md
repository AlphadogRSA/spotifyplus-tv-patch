# Handoff

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

The extension's added network request is only the explicit LRCLIB lookup; no Spotify login credentials or entitlement values are processed by it. Source code and tests remain MIT licensed. See README.md, LYRICS.md and assets/INSPIRATION.md for scope and notices.
