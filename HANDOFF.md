# Handoff · Spotify+ TV 1.3.0.1

User requirements: one Spotify APK, integrated Hush functionality, reference-style graphic EQ, and all custom controls inside Spotify’s existing settings panel.

Implemented: native `/profile` settings host integration using observed `profile-menu`/`profile-menu-item` and `top-menu--settings` selectors; restricted JavaScript settings bridge; native graphic EQ and migration; embedded pinned Hush helper/provider with fixed restart command; persisted sleep/recovery state; single unsigned APK rebuild and retained-key signing/install scripts.

The rebuilt file still targets the inspected ARMv7/English/TV-density installation, with versionName `1.3.0.1` and original versionCode `2340200`. The source repo excludes private signing material. An unsigned build is not an installable same-key update. No installation or live-TV verification for this version is claimed. Earlier 0.5 observations remain historical in VERIFICATION.md.

Read HUSH.md for setup and recovery limitations, README.md for build/install scope, and the current plan/spec under docs/superpowers. The native Settings selectors were verified against Spotify’s delivered Android TV web UI `ad3ec52`; future remotely delivered UI changes may need selector updates.
