# Hush support for Spotify+ TV

The user approved adding Hush as a TV app feature, using a companion to survive Spotify restarts. Add a remote-friendly Hush menu entry and an independently installed companion derived from coldMEW/Hush 2.0.2 at `4e9ded397338de5c69b1784c636feb3d34a8de01`. Preserve themes, lyrics, EQ and player-local muting. Persist the TV sleep deadline across process restarts.

The companion uses Hush's existing helper/Shizuku restart engine. TV native ad flags and own-session tokens cross an explicit, signature-protected receiver; both APKs must be signed with the user's existing key. No arbitrary shell commands or account changes are added. Native session forwarding avoids depending on TV notification access. Restart skipping is opt-in, deduplicated, and falls back to existing local muting when unavailable. Never use Hush's phone-wide volume mute on TV.

Approaches considered: embedding everything loses the helper on each Spotify restart; a separately signed companion survives and keeps GPL code separate from the MIT extension. The user chose the companion approach. Keep upstream as a pinned submodule and apply explicit reproducible TV changes in a build preparation script, with attribution and GPL source available. Do not copy GPL implementation into the MIT extension.

The first version exposes the existing Hush wireless-debugging setup and Shizuku authorization from a D-pad screen; device-specific pairing and TV restart behavior require real-device checks. DNS remains off by default and is available only in advanced upstream settings. CI builds the companion; rebuilding the proprietary Spotify splits still requires the user's original four APKs and private key, neither available here.
