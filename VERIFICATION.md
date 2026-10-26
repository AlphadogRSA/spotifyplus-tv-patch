# Verification — 26 October 2026

## Lyrics update 0.2

- The new `build.ps1` entrypoint completed from the original four-APK input set, compiled the Java extension and D8 dex, applied the pinned hooks, and verified all four output signatures.
- The signed 0.2 APK set installed successfully as an update over the existing local patch. No uninstall or app-data reset was used for this update. Cold launch returned `Status: ok`.
- Lyrics parser/timing: **13 assertions passed**. LRCLIB query/response parsing: **10 assertions passed**. Fixtures contain only original test text, not fetched commercial lyrics.
- Optional `ProviderLiveTest` completed an actual HTTPS request using metadata from LRCLIB's public documentation and received both timed and plain results. Lyric text was not printed or committed.
- Python patch/import tools passed compilation. The existing injected JavaScript passed syntax checking. A source-only GitHub Actions workflow includes both deterministic Java tests and Python/JavaScript checks; it excludes the online smoke test.
- Independent review checked stale-result handling, local precedence, bounded cache and network behavior. The review's slow-response concern was fixed with an overall watchdog plus monotonic deadline checks and read/connect limits.
- No Eevee source or assets are copied. The new provider code is an independent implementation against LRCLIB's public contract. The source repository excludes proprietary APKs, signing keys and fetched lyric data.
- On the TV after updating: the new lyrics panel rendered with fetch/plain/size/timing controls. Spotify was already signed in by the user. A manual LRCLIB lookup for the currently exposed native track returned timed lyrics and displayed the surrounding lines at the native playback position. No Play/Pause command or sign-in action was performed by the agent for this check. No fetched lyrics, account screenshots or account metadata are included in the GitHub source.
- The plain-text dialog opened, and a D-pad Down event scrolled it. D-pad navigation reached the size and timing controls, changed them to 36 sp / +500 ms, and saved those values. Defaults were restored to 30 sp / 0 ms afterward, and the overlay was closed.

The historical initial-patch checks below remain relevant to the base hooks. The 0.2 lookup/render flow was observed on a real signed-in track; extended drift, seek/pause/resume and track-change-during-fetch coverage still needs further testing.

## Initial patch 0.1 — actual result

The patched Spotify TV installation replaced `com.spotify.tv.android` on the connected Xiaomi MiTV-AESP0 (Android API 29). All four required APKs were installed together; the final update installed successfully with `adb install-multiple -r`. Cold launch returned `Status: ok`. Spotify's own login screen renders and remains available for the user to sign in. No Spotify account was signed into by the agent, and live audio playback was not tested.

## Checks passed

- Original installed base/ARMv7/English/TV-density split APKs backed up before uninstalling the official package. This backup does not include account data or login state.
- Original Java extension compiled for Java 8 with UTF-8 encoding; D8 compiled the injected dex with min API 26.
- The original base APK SHA-256 was checked before patching. Five hook locations were patched: activity creation, destruction and key dispatch; TV WebView page completion; own media-session creation.
- All four final APK signatures verified with Android SDK apksigner for the target Android generation. v2/v3 signing protects the APK contents. The earlier full v1 check also emitted informational warnings about original META-INF metadata entries; no signature verification failure occurred.
- Final APK ZIPs and downloadable archives checked for CRC/integrity.
- Java lyrics parser and progress calculations: **13 assertions passed**, including fractional timestamp precision, repeated timestamps, offsets, active-line boundaries, paused progress, interpolation and duration clamping.
- Injected JavaScript parsed successfully in Node.
- On the actual TV: Menu and repeated long-OK/release opened the injected native settings panel; D-pad focus and short OK activated its controls.
- Theme cycling and motion toggles saved to the extension's private preference file and survived APK updates. Final settings are Aurora theme with motion enabled.
- The gradient theme was visibly rendered on Spotify's original login screen. Extension logs returned successful CSS application results.
- The patch captured the same process's own native media-session token, confirmed by its diagnostic log. It never enumerates or controls other apps' media sessions.
- The native lyrics overlay opened. The import helper atomically installed an original test LRC into app-private storage, and its first line appeared in the active-line position. The test file was removed afterward.
- Sleep timer option dialog opened with 15/30/60-minute/cancel choices. It was dismissed without leaving a timer scheduled.
- Independent static review identified the initial long-OK handoff issue; the revised balanced short-press forwarding and release-triggered long-press logic was reviewed without residual significant findings.

## Not verified / limitations

- Account login completion, entitlement/certificate-bound services, actual audio playback, Cast and Play Store update compatibility have not been verified with an authenticated Spotify account. The patch does not bypass any checks if Spotify rejects it.
- Timer expiration and a real pause action were not tested against playing music. Timing is process-local and is lost if Spotify is killed/restarted.
- Lyrics rendering/import and timing arithmetic were checked; live synchronization to an actual track is untested until sign-in/playback. There is no online or word-timed lyrics catalog.
- Themes can be affected by Spotify's remotely loaded web UI. The CSS only targets allowed Spotify pages and large dark panels; coverage outside the observed login screen is unverified.
- This is a tested experimental subset, not a complete TV port of all mobile SpotifyPlus features. See README.md for explicit omissions.
- The build is intentionally debuggable to support private LRC import through trusted ADB. It uses a local development signing key. The original app's login was cleared by the necessary uninstall/reinstall.

The TV was left at Spotify's regular login screen after checks. No login secrets were collected. The source ZIP contains only the original extension, patcher, helper, tests and notices; proprietary installation files are separate personal-device deliverables.
