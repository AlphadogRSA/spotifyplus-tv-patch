# 1.3.0.7 visible-navigation verification - 27 October 2026

- Regression fixtures reproduced hidden/transparent/offscreen focus and an unrepaired native Upgrade position, then passed after visibility filtering, endpoint clamping and menu-entry focus repair.
- Browser tests verify dynamic removal of the selected control, Upgrade disabled/tabIndex -1/aria-hidden, preserved account/Now Playing access, Settings/Home focus handoff and bounded scrolling. Theme and efficiency regressions passed.
- Same-key 1.3.0.7 installed on Xiaomi TV. Eleven actual Right presses and eleven Left presses kept selected controls visible and clamped at Account and Now Playing.
- Simulated Spotify's remembered Upgrade focus at menu entry; it immediately moved to a visible control. Upgrade was disabled, hidden, excluded from tab order and computed to display:none.
- Home still activated. No uninstall or data clearing; pre/post third-party package lists unchanged. Helper startup requested after update.
- Final APK signatures/certificate and alignment, installer validation and archive integrity checked. Audio response and full Hush recovery were not retested.

# 1.3.0.6 focus-handoff verification - 27 October 2026

- Regression test first reproduced Home OK being swallowed by a profile setting. Further cases reproduced stale unmarked header focus on profile reentry and a dialog consuming the prior OK release.
- Shared browser/native key dispatch now prioritizes top navigation, clears stale profile/header focus, delegates Up from the first profile row and keeps matching key-release handling.
- Focus handoff, native settings, Upgrade blocking, bounded scroll, theme, bridge lifecycle and presentation-efficiency regressions passed.
- Final APK assets match source; signatures and retained certificate verified, alignment checked, and 1.3.0.6 installed as a same-key update on the Xiaomi TV.
- Three real remote About-open/Back/Home-OK cycles each activated Home exactly once and removed injected profile rows on leaving.
- Real remote Up released the profile to the top menu; native profile box shadow was none. Left/Right reached Home, and OK activated it exactly once. Upgrade click remained blocked.
- Other installed apps unchanged; no uninstall or data clearing. Hush helper startup requested after update.
- Audio response and full ad-recovery/queue behavior were not retested. Historical evidence follows.

# 1.3.0.5 verification - 27 October 2026

- New regressions reproduced direct Upgrade activation and missing new palettes before implementation, then passed for click blocking, virtual-focus OK, Left/Right skipping, upsell-route redirection, account/Now Playing access, and Ocean/Rose/Solar colors.
- Native bridge tests verify handled keys never replay and unhandled keys replay exactly once; Back and untrusted routes retain their boundaries. Existing settings/scroll, theme and efficiency regressions pass.
- Same-key APK installed on Xiaomi TV as 1.3.0.5. Direct Upgrade click stayed on Home, forced upsell route redirected Home, and real remote Left/Right events skipped Upgrade and reached settings/account controls.
- Real remote OK cycled all seven themes; the prior selection was restored.
- Remote OK opened About. Developer Tumelo Rathebe / AlphadogRSA, public GitHub URL and version verified in native UI hierarchy.
- QR generated offline and independently decoded both from the PNG asset and from the actual TV About screenshot to https://github.com/AlphadogRSA.
- Signature/certificate, alignment and package metadata checks passed. Other installed apps unchanged; no uninstall or app-data clearing.
- Audio response, full ad-recovery/queue behavior and account service compatibility were not retested in this update.

# 1.3.0.4 UI verification - 27 October 2026

- Regression fixtures first reproduced the outer-header scroll offset and the first-frame Upgrade control visibility, then passed with bounded inner scrolling and direct CSS hiding.
- Settings, Upgrade entry, native navigation, presentation, theme regressions, layout efficiency and native bridge lifecycle tests passed.
- APK compiled with the pinned helper; signature/certificate, alignment and version metadata checks passed.
- Same-key update installed on Xiaomi MiTV-AESP0 as 1.3.0.4. Real ADB D-pad events reached all 11 settings rows and kept the selected control visible.
- Header position remained unchanged during a full downward/upward traversal and after returning Home. Main Upgrade controls computed to display:none with zero rendered width.
- Pre/post third-party app lists unchanged; no uninstall or data clearing. Helper startup requested after the update.
- Audio quality and Hush ad-recovery/queue behavior were not retested. Older evidence below retains its original scope.

# 1.3.0.3 remote-navigation verification - 27 October 2026

- Passed native bridge lifecycle/routing and browser settings regression tests, including virtual focus, key-up suppression and rejection outside trusted profile routes.
- Passed 12 presentation checks, 16 theme regressions and 4 presentation-efficiency checks.
- Compiled the updated runtime and pinned Hush helper; APK v1/v2/v3 signatures and prior certificate matched; alignment verified.
- Installed on Xiaomi MiTV-AESP0 with adb install -r; Android reports versionName 1.3.0.3.
- Real ADB D-pad events reached all 11 custom settings rows; selected controls remained within the viewport allowing subpixel rounding.
- OK on Graphic equalizer opened the native EQ dialog; native Back dismissed it.
- App data retained by same-key update; pre/post third-party package lists unchanged. Hush helper startup requested after update.
- Audio output, ad recovery and queue behavior remain unverified for this build. Prior Gradle fingerprint invocation was blocked by a local Java loopback error.

The historical evidence below describes older builds and retains their original limitations.

# Version 1.3.0.1 — verified source and unsigned package (2026-10-27)

- Final functional source: `c09378bfbf1bd12a3ff8d51453b4e489d256999e`. GitHub Actions run `40503784827` passed all three jobs: deterministic/helper/bridge/fingerprint checks, browser presentation/native-settings regressions, and Android compile/DEX/single-APK rebuild.
- Local Java checks passed: lyrics 13, provider 10, ad-mute state 13, audio-session isolation 7, existing EQ/cache 10, plus graphic curve interpolation/migration/clamping/cache, sleep deadline and Hush policy cases. The first-document WebView lifecycle test passes without supplying a JavaScript bridge in advance; it checks null-URL registration, trusted first-page invocation and rejection after untrusted navigation.
- Browser checks passed: 12 presentation, 16 theme/promotion, 4 performance checks, plus native settings route scoping, original-row preservation, D-pad entry/return, actions, state refresh and cleanup.
- Review fixes: the bridge is now injected immediately before both observed native WebView `loadUrl` calls; recovery no longer has a process-local restart latch that can remain stuck if the helper fails to terminate its caller. The pending sleep deadline remains independent of visible timer cleanup.
- The local rebuilt single APK reports package `com.spotify.tv.android`, versionName `1.3.0.1`, versionCode `2340200`; ARMv7 player libraries are merged. Earlier extension definitions are removed, the new runtime classes occur once, and the guarded embedded helper provider/full GPL notices are present. There is no companion app.
- Final local APK SHA-256: `202c36000fb1579e3020ffb82ff86155bc8fe0cab508da5b07c0088fdb66c1f5`. Android apksig verification confirms **unsigned, zero signer certificates**. It must be signed using the retained TV key before same-key installation; no new signing key was substituted.
- Not verified: installation/update on the TV, live D-pad behavior in Spotify’s complete SPA, actual frequency/loudness response, real ad-break dropping, queue context, process-restart playback, background/screen behavior and helper setup after reboot. No live-TV success claim is made for this version.

## Historical 0.5 and earlier observations

# Verification — 26 October 2026

## Release 0.5

- Reproduced the theme issue: the TV stylesheet hard-codes focus/green colors and uses `--focused` classes; it does not consume the desktop color tokens previously set by the extension. Regression tests failed on actual TV selector fixtures before the fix. The new rules target those TV states without replacing artwork or layout.
- The final 0.5 four-APK set was rebuilt, aligned and signature-verified, installed over 0.4 without uninstalling, and cold-launched with Status: ok. On the TV, native remote theme switching changed the current tile outline to Aurora rgb(145,231,207), Midnight rgb(187,179,255) and Ember rgb(255,191,152). Spotify Original restored white. The previous Ember selection was restored after this check.
- Promotion filtering reported three matching elements on the live page and zero visible Upgrade labels. The native settings and lyrics dialogs also had no visible Upgrade labels. Existing navigation and playback controls were retained. User preferences, including their artwork-blur choice and audio settings, were preserved.
- Local browser checks: 12 existing presentation checks, 16 theme/promotion regression checks and 4 layout-efficiency checks passed. They cover native focus/pseudo-elements, progress/playing colors, primary buttons, broad labels/banners, unrelated content, reused/detached nodes, reversible settings, foreground artwork and idle work.
- In a 1,000-card browser fixture, repeating unchanged settings 20 times caused 20,060 geometry reads in 0.4 and zero in 0.5. Focus/progress-only and artwork-opacity-only updates caused zero geometry reads in 0.5. This measures extension work in that fixture, not whole-app frame rate or device battery use.
- Native Java checks passed: 13 lyrics, 10 provider, 13 ad-state, 7 audio-session isolation and 10 EQ settings/cache assertions. The EQ cache test verifies zero additional hardware writes across 100 identical updates, targeted changed-band writes, device-range clamping, control invalidation and failure retry. The Kotlin fingerprint suite passed 16 checks during the build.
- On-device playback activated Spotify-session audio effects after the callback/caching changes. Lyrics received current cached metadata and enabled the manual provider lookup. Dialog cleanup and callback replacement were independently reviewed; fast lyric interpolation and process-local sleep timer behavior remain implemented. Ad timeline behavior is unchanged; no new audible live-ad verification was performed.

## Release 0.4

- The modular ReVanced build applied to hash-verified original version 1.134.2 inputs, merged the runtime extension, rebuilt resources, aligned all four APKs and verified every signature. Final output was installed with install-multiple -r and the existing local key. Cold launch returned Status: ok. No uninstall or app-data reset was performed.
- Deterministic checks: 13 lyrics parser/timing assertions, 10 provider assertions, 13 ad-muting/restoration assertions, 7 audio-session isolation assertions and 16 Kotlin fingerprint/safety checks passed. The optional local Playwright/Edge presentation test passed 12 checks, including Upgrade hiding/restoration, dynamic insertion, fullscreen-background blur and clear foreground text/artwork. Python helpers compiled successfully.
- The Android 10 diagnostic permission grant succeeded. On the TV, the audio panel reported Active · Spotify audio only while EQ/boost were enabled. Equalizer and LoudnessEnhancer initialization and settings returned successfully for this process's positive audio session. No shared-output effect was requested. The existing D-pad settings were retained across updates; the user adjusted audio/theme controls during testing.
- The native extension menu, matching accent color, artwork blur and Upgrade controls rendered on the TV. Live WebView checks returned blur=1 and upgrade=1 on applicable pages. Counts can be zero on pages without matching artwork/Upgrade controls. The actual page’s fullscreen absolute artwork image was inspected to refine the background-only selector; smaller cover artwork is preserved.
- Independent review found a lifecycle cleanup gap and an insufficiently strict diagnostic table header check. Both were fixed before the final build: activity destruction invalidates pending work and queues effect release; unfamiliar/reordered table layouts are rejected. No account data or diagnostic dumps are committed.
- Ad muting is implemented and tested with synthetic state transitions, including disabling, volume changes and stale-state restoration. A real audible ad-break cycle has not been verified. Muting does not skip break duration. Audio attachment/settings were observed; no calibrated loudness or frequency-response measurements were made.
- Original login and service behavior remain Spotify's. The user paired again during this session. Themes and audio settings remain app-private and are retained by same-key updates. No DNS/VPN filter, ad SDK removal, subscription spoofing or streaming replacement is included.

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

The TV was left at Spotify's regular login screen after checks. No login secrets were collected. The source ZIP contains only the original extension, patcher, helper, tests and notices; proprietary installation files are separate device-specific deliverables.
