# EQ and volume boost

Open Menu (or hold OK), then **Audio · Equalizer & Boost**. Effects are off by default. Enable EQ & Boost, use Up/Down to choose a band, and Left/Right to adjust. OK increases the selected value. Five controls target 60 Hz, 230 Hz, 910 Hz, 3.6 kHz and 14 kHz, from −6 to +6 dB. Hardware EQ bands use the nearest target frequency and respect the device’s supported range. Boost uses Android LoudnessEnhancer from 0 to +6 dB. Flat/reset restores zero EQ gain and zero boost. Settings persist.

Effects attach only to positive audio session IDs belonging to this Spotify process. There is **no output-mix/session-0 fallback**. They are released when playback pauses, the owning activity is destroyed, or the session disappears. Discovery runs on a background worker, with bounded diagnostics and a timeout. Headroom and effect support depend on the TV/output; this does not replace hardware volume controls or add a streaming engine.

This TV client does not publish its audio session through a public app callback. This Android 10 build therefore uses the platform’s diagnostic `media.audio_flinger` track table. After installing the four APKs, perform the one-time development permission setup over authorized ADB:

```powershell
adb -s TV_IP:5555 shell pm grant com.spotify.tv.android android.permission.DUMP
```

`install.ps1` performs installation and this setup together. Android’s DUMP permission can expose system diagnostic information to the app; this extension only parses its audio diagnostic output in memory to find rows with the current process PID. It never saves or transmits that output. Effects remain off and show a setup/status message if permission, the verified Android 10 table format, or device effects are unavailable. The code does not guess another app’s session. Equalizer and LoudnessEnhancer use Android’s public audio-effect APIs.

To revoke the development permission:

```powershell
adb -s TV_IP:5555 shell pm revoke com.spotify.tv.android android.permission.DUMP
```

The implementation and sideload setup were tested on the inspected Xiaomi Android 10 TV. Diagnostic table layouts are not a stable Android app API; unfamiliar layouts are rejected rather than attaching to an unverified session.
