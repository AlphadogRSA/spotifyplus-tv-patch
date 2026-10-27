# Spotify+ TV 1.3.0.1 design

The user requests one installed Spotify app with Hush functionality embedded, a graphic EQ matching the supplied dark/blue reference, custom controls inside Spotify’s existing settings panel, and versionName 1.3.0.1.

Embed only eight GPL helper classes from coldMEW/Hush 2.0.2 at 4e9ded397338de5c69b1784c636feb3d34a8de01. Start the shell-owned helper through authorized ADB, so it survives Spotify force-stop. Restrict its adapted command builder to the fixed Spotify TV restart. Persist recovery state before stopping; the new app process recovers its own media session with time bounds, cooldown and sleep protection. Keep local muting as fallback. No companion application, VPN, phone UI or account/entitlement changes.

Place all entry controls in the observed native profile settings menu, preserving Spotify rows and its focus styling. Restrict the WebView bridge to fixed settings actions on allowed Spotify main pages. The shortcut opens native Settings. EQ/lyrics/timer are native subpanels reached from these rows.

The EQ has ten visual curve points (31 Hz–16 kHz), log-frequency interpolation onto real output bands, ±6 dB range and 0–6 dB post-gain. Preserve five-band settings via migration. Provide remote, touch and accessibility adjustment.

Rebuild the exact prior release, replace only extension classes/assets, add the embedded provider and merge the inspected splits into one unsigned APK. Retain versionCode 2340200 for the base client. The retained private signing key is unavailable; do not claim an installable same-key update or device verification without it.
