# Lyrics update 0.2 — implementation handoff

The Eevee-inspired additions are independent Android implementations: manual LRCLIB lookup, timed/plain fallback, lyric-size controls and timing correction. No GPL-licensed Eevee implementation was copied. No subscription/authentication changes are included.

## Flow

1. The injected media-session hook captures only Spotify's own native token.
2. `Hooks.updateLyrics()` reads that session's metadata and interpolates its playback clock.
3. A valid local `files/sptv-lyrics/NAME.lrc` takes priority.
4. **Fetch from LRCLIB** snapshots current title, artist, album and duration and runs `Provider.fetch()` on a background executor. The overlay states what metadata is shared.
5. `GET https://lrclib.net/api/get` uses the public metadata contract. No Spotify token/client ID is used. Redirects are disabled, response size is capped at 512 KiB, connect/read timeouts are set, and a 30-second overall watchdog disconnects slow responses. A monotonic deadline is also checked between reads.
6. Results are returned to the UI thread and stored under the captured track identity in a 12-entry memory map. Rendering always resolves the currently playing identity, so a late response for an old track does not replace the new track's lyrics. Requests have a 30-second cooldown.
7. Timed LRC uses the parser and current native playback position. Plain results display a preview and can be opened in a separate D-pad-scrollable dialog. Instrumental/no-result/error responses are explained without inventing timings.

## Main files

- `src/dev/aurora/tvpatch/Hooks.java`: injected lifecycle/session hooks, dialogs and provider integration.
- `src/dev/aurora/tvpatch/Provider.java`: public LRCLIB client and response parser.
- `src/dev/aurora/tvpatch/Lyrics.java`: LRC parser and clock interpolation.
- `assets/spotifyplus-tv.js`: theme presentation only.
- `patch.py`: exact-version bytecode injection; rejects an unrecognized original base APK.
- `build.ps1`: reproducible Windows build/signature verification from the user's four original APKs.
- `tests/`: original lyric fixtures, query/response tests, optional live lookup smoke test.

## Continue development

The 0.2 update was observed fetching and rendering timed lyrics for a real signed-in Spotify track at its native playback position. Continue checking track changes during a fetch, local-file precedence, seek/pause/resume, long-running drift and plain-dialog scrolling. Do not treat that single lookup/render check as complete device coverage. LRCLIB coverage and timing can vary.

Fetched lyrics are not saved to disk. If adding persistence later, define retention and deletion and preserve provider/rightsholder attribution. Never commit proprietary APKs, signing keys, account tokens or fetched lyric catalogs. GitHub contains the extension source and build/test instructions only.
