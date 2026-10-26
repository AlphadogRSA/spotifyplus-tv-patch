# SpotifyPlus TV experimental local patch 0.2

This is an actual extension injected into the user's Spotify Android TV **1.134.2** installation (`com.spotify.tv.android`), rather than the separate Aurora companion. It is version-specific and intended for personal testing on the inspected Android 10 TV.

## Included

- An in-app native TV settings panel opened with **Menu**, **F1**, or **hold OK/Select and release**.
- Three custom themes (Aurora, Midnight, Ember) and Spotify's original/default appearance.
- A slow cinematic CSS gradient with motion on/off. Styling targets large dark page panels; its coverage can vary as Spotify changes its remotely loaded TV UI.
- A sleep timer (15/30/60 minutes/cancel), using the current Spotify process's **own Android media-session token** and `MediaController.TransportControls.pause()`.
- A native line-synchronized lyrics overlay for **your own or licensed local LRC files**, with a Play/Pause button targeting the same Spotify session. Timing reads the native media session and interpolates progress.
- Optional **Fetch from LRCLIB** lookup for the current track, timed lyrics when available, and a scrollable plain-lyrics fallback. Lookups are manual, use HTTPS and send only track title, artist, album and duration to `lrclib.net`.
- Lyrics text sizes of 24/30/36 sp and a persistent timing correction from −1000 to +1000 ms. Positive timing advances the highlighted line.
- Larger existing TV lyric text where Spotify's own web UI uses compatible lyric markers.

The patch keeps Spotify's login, audio engine, entitlements and DRM. It adds no streaming implementation, private Spotify API requests, premium bypass or token harvesting. No developer Client ID is needed: use Spotify's normal TV login. LRCLIB is an optional external service; its lyrics remain subject to rightsholders' rights and provider availability.

## What is not ported

This is a first TV-specific subset, **not the full mobile SpotifyPlus module**. It does not supply a commercial lyrics catalog, word-by-word vocals, translations, animated album-art catalog, Last.fm integration, persistent private-session enforcement, restored mobile Like button, or modified queue/history behavior. Original playback and queue features remain Spotify's own. Local LRC synchronization is line-level and depends on the metadata/progress Spotify exposes to its media session.

## On the TV

Open **Spotify TV +**, sign in through Spotify, then press Menu or hold OK and release. Short OK presses are forwarded as a balanced down/up pair to the original app. Use D-pad and OK to choose settings; Back closes a dialog. Themes and motion settings persist; the sleep timer is process-local and does not survive a Spotify process restart. Keep Spotify running until it expires.

This installation required replacing the signed official package. Its previous local sign-in was not preserved. Signing in again is expected. Future Play Store updates cannot normally update this build because the certificate differs; restoring the official client also requires reinstalling it. The Play Store/native platform can reject a modified build or limit certificate-bound features such as Cast; those are not bypassed here.

## Import local lyrics

Open **Lyrics · local / LRCLIB** from the injected menu. Press **Fetch from LRCLIB** to request lyrics for the current song. Timed results use the existing playback clock; if only plain lyrics are available, choose **Plain lyrics** and use Up/Down to scroll, Back to close. Valid local LRC files take priority. A request cooldown and overall timeout prevent repeated or indefinitely pending lookups. No lookup happens automatically on track changes.

Only the most recent 12 fetched records are kept in process memory. They disappear when Spotify's process exits. No Spotify token or account identifier is sent to LRCLIB. See [lyrics behavior and handoff notes](LYRICS.md).

Start the desired song, open **Lyrics · local / LRCLIB**, and note the exact filename shown when no local lyrics are loaded. Usually it is the Spotify track's 22-character ID plus `.lrc`; if no ID is exposed, the overlay shows a `track-...lrc` fallback derived from title and artist.

Create a UTF-8 `.lrc` using words you have permission to use:

```lrc
[offset:0]
[00:00.00]The room is still
[00:05.00]The night unfolds
[00:10.00]A little light to lead us home
```

With ADB authorized:

```powershell
python import_lyrics.py "C:\My Lyrics\song.lrc" --name EXACT_FILENAME.lrc --serial TV_IP:5555 --adb "C:\platform-tools\adb.exe"
```

The helper writes atomically into the app-private `files/sptv-lyrics` directory using debug `run-as`. It does not write to shared TV storage or read login data. This personal build is deliberately debuggable to support this import; disable TV debugging when not in use and authorize only trusted PCs.

## Package and restore

The `.apks` download is a ZIP containing **four APKs**: patched base, ARMv7 native-library split, English split, and TV-density split. It is specific to the installation that was inspected. Do not install only the base APK or mix differently signed splits. It is not a universal standalone APK for other devices.

Extract and install all four with Platform Tools:

```powershell
adb install-multiple -r base.apk split_config.armeabi_v7a.apk split_config.en.apk split_config.tvdpi.apk
```

For a first install over the official package, Android's signing rules require removing that package first. **Uninstalling clears local login/settings.** To restore the official client, uninstall the patched package and install Spotify again from the TV's store, or extract the separately saved original-installation backup ZIP and install its four original APKs together. That backup contains installation files, not the previous account session.

## Rebuild the patch

The checked Windows build entrypoint is `build.ps1`. It checks all four input APK hashes, compiles the Java extension, runs D8, invokes the version-pinned patcher, aligns and signs each APK, and verifies the signatures. It requires a new output directory and makes no TV changes:

```powershell
$env:SPTV_STORE_PASSWORD = 'YOUR_KEYSTORE_PASSWORD'
# Set SPTV_KEY_PASSWORD too if the key password differs.
.\build.ps1 -OriginalDirectory C:\private\original-apks -AndroidSdk C:\Android\sdk -JavaHome C:\Java\jdk-17 -ApktoolJar C:\tools\apktool.jar -KeyStore C:\private\signing.keystore -KeyAlias YOUR_ALIAS -OutputDirectory C:\builds\sptv-02
```

Keep your signing key private and out of GitHub. The existing personal build's key was retained locally, and is not in this repository. A new signing key requires reinstalling the package and signing into Spotify again.

Inputs: original four APKs from your own TV, JDK 17, Android SDK Platform 35/Build Tools 35.0.0, Python 3.10+, and [Apktool 2.11.1](https://github.com/iBotPeaches/Apktool/releases/tag/v2.11.1). No proprietary original APK is included in this source ZIP. The original backup and patched installation set are separate personal deliverables; do not publish them as MIT software.

1. Compile `src/dev/aurora/tvpatch/*.java` with `javac -encoding UTF-8 -source 8 -target 8 -cp ANDROID_SDK/platforms/android-35/android.jar -d classes`.
2. Package these classes with `jar cf extension.jar -C classes .` and run SDK D8 with `--min-api 26 --lib android.jar --output dex extension.jar`.
3. Run `python patch.py --base ORIGINAL/base.apk --apktool apktool.jar --java PATH_TO_JAVA --work NEW_BUILD_DIRECTORY --dex dex/classes.dex --framework FRAMEWORK_CACHE_DIRECTORY`.
4. The script verifies the inspected base APK SHA-256, decodes it, inserts five version-specific hook locations, adds the extension dex and assets, and rebuilds an unsigned APK. Mismatched original inputs are rejected. `--work` must be new; nothing is recursively overwritten.
5. Zip-align the modified base and all original splits (`zipalign -P 16 -f 4`), then sign **all four with the same personal key** using `apksigner`. Verify every APK before installing. Retain that key privately if you want to update your patched installation without uninstalling it.

Local parser/timing tests:

```powershell
javac -encoding UTF-8 -d test-classes src/dev/aurora/tvpatch/Lyrics.java tests/LyricsTest.java
java -ea -cp test-classes LyricsTest
```

Provider parser/query tests are in `tests/ProviderTest.java`; they use JSON-java 20240303 as a **test-only** dependency. The runtime uses Android's built-in JSON implementation. `.github/workflows/test.yml` downloads the test JAR from Maven Central, verifies its SHA-256, then runs both deterministic test programs. `ProviderLiveTest` is an optional online smoke test and is excluded from normal CI.

## Privacy and licensing

The extension's only added network operation is a user-initiated HTTPS lookup to LRCLIB with song metadata. It does not read OAuth/session secrets. Spotify's original networking/analytics remain those of the original application. Local preferences and imported LRC files remain app-private. Fetched records are held in bounded process memory. Clearing app data or uninstalling removes local files/settings. The own-session token stays in process memory and is never exported or printed. Extension diagnostic logs contain only attach/capture state and its own CSS result, not account data or lyric text.

The original extension source is MIT licensed. It is inspired by [LeNerd46/SpotifyPlus](https://github.com/LeNerd46/SpotifyPlus), MIT, © 2025 Devon Shoutz; the notice is preserved in `assets/SPOTIFYPLUS-LICENSE.txt`. No upstream fonts, artwork, commercial lyrics, or mobile hook implementation are bundled. Spotify's app, native libraries, artwork and branding retain their own rights. The MIT license applies only to the extension's original source, not the patched proprietary application.

The lyrics update is also inspired by [EeveeSpotifyReincarnated](https://github.com/SideloadLabs/EeveeSpotifyReincarnated). Eevee is GPL-3.0 licensed and targets iOS. No Eevee source, assets or hooks are incorporated. The LRCLIB client is independently written against [LRCLIB's documented API](https://lrclib.net/docs). See `assets/INSPIRATION.md`.

See VERIFICATION.md for actual checks and outstanding account/playback testing.
