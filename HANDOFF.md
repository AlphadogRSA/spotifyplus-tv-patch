# Current update: 1.3.0.8

Home blur now adds an isolated halo behind native artwork and never uses page-level/native Home wrappers as a clipped fallback. Loading/fade-in, toggle cleanup and native geometry verified. PanelUi provides the EQ-style navy/blue native frame, buttons and dialogs; settings cards match. Final native panels and QR visually/device tested.

# Previous update: 1.3.0.7

Home navigation now includes only visible eligible controls, clamps at visible endpoints, disables Upgrade for focus/accessibility, and repairs a remembered Upgrade position on menu entry. Verified actual remote traversal and Home activation on the Xiaomi TV.

# Previous update: 1.3.0.6

Shared key routing fixes stale Spotify+ selection and missed Home OK. Native dialogs no longer leave a release state that swallows the next press. Verified three About/Home cycles plus remote top-menu handoff; current source/build remains on feat/hush-tv.

# Previous update: 1.3.0.5

Upgrade UI activation is blocked at click, native remote navigation and SPA upsell-route levels. About includes Tumelo Rathebe / AlphadogRSA with an offline GitHub QR. Theme cycling now includes Ocean, Rose and Solar, preserving existing theme indices. The installed same-key build and QR screenshot were verified on the TV.

# Handoff · Spotify+ TV 1.3.0.1

User requirements: one Spotify APK, integrated Hush functionality, reference-style graphic EQ, and all custom controls inside Spotify’s existing settings panel.

Implemented: native `/profile` settings host integration using observed `profile-menu`/`profile-menu-item` and `top-menu--settings` selectors; restricted JavaScript settings bridge; native graphic EQ and migration; embedded pinned Hush helper/provider with fixed restart command; persisted sleep/recovery state; single unsigned APK rebuild and retained-key signing/install scripts.

The rebuilt file still targets the inspected ARMv7/English/TV-density installation, with versionName `1.3.0.1` and original versionCode `2340200`. The source repo excludes private signing material. An unsigned build is not an installable same-key update. No installation or live-TV verification for this version is claimed. Earlier 0.5 observations remain historical in VERIFICATION.md.

Read HUSH.md for setup and recovery limitations, README.md for build/install scope, and the current plan/spec under docs/superpowers. The native Settings selectors were verified against Spotify’s delivered Android TV web UI `ad3ec52`; future remotely delivered UI changes may need selector updates.
