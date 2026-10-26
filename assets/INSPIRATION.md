# Source and feature attribution

SpotifyPlus TV uses original Java/Python/JavaScript code written for the Android TV 1.134.2 client. The extension is MIT licensed; Spotify's proprietary client, branding and assets are not covered by that license.

SpotifyPlus by LeNerd46 (MIT, copyright 2025 Devon Shoutz) inspired themes, motion, sleep timer and custom lyrics. Its MIT notice is preserved in SPOTIFYPLUS-LICENSE.txt. No upstream mobile hooks, fonts, music, artwork or lyrics catalog are copied.

EeveeSpotifyReincarnated by SideloadLabs inspired the optional custom-lyrics provider and presentation controls: https://github.com/SideloadLabs/EeveeSpotifyReincarnated . That project is an iOS tweak licensed under GPL-3.0. No Eevee source code, binaries, assets or iOS hooks are copied or linked into this extension. This is an independent Android implementation of generic lyrics features, not a port or relicensing of Eevee.

The LRCLIB client is independently implemented against the public API contract at https://lrclib.net/docs . Only a user-initiated lookup sends current track title, artist, album and duration over HTTPS to lrclib.net. No Spotify credentials or subscription data are sent. Lyrics belong to their respective rightsholders; provider availability and permissions are not granted by this source code's MIT license.
