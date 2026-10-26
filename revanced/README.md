# Modular TV patches

This host-side Kotlin project uses official ReVanced Patcher 22.0.0 and Library 4.0.1 from the SHA-256 pinned official CLI 6.0.0 distribution. `bootstrap.py` verifies the download. Java 17 and Gradle 8.10.2 are required; the wrapper supplies Gradle. No GitHub Packages credentials are needed.

`build.ps1` first compiles the independent MIT Java runtime to DEX, then runs `testFingerprints bundle -PextensionDex=ABSOLUTE_DEX_PATH`. The resulting `build/libs/spotifyplus-tv-0.4.rvp` contains Kotlin patches, theme/notices resources, and `extensions/spotifyplus-tv.rve` (the compiled DEX). Build inputs are your original four version 1.134.2 APKs, checked against `original_hashes.json`. Keep APKs and signing keys out of Git.

The default aggregate depends on lifecycle, remote menu, WebView, own-session, and ad-muting bytecode patches. They depend on runtime-extension merging and a preflight that rejects existing extension classes and missing/ambiguous matches. Manifest changes use `resourcePatch`; assets use `rawResourcePatch`; bytecode changes use `bytecodePatch`. The current API uses `apply` blocks. The runner accepts explicit named selections and lets the official patcher resolve dependencies.

Fingerprints combine signatures, stable marker strings and instruction references. Obfuscated callback/owner names are not hard-coded. Public Android callbacks and the player volume interface retain meaningful method names. Ad-state injection observes the result immediately after JSONObject.getBoolean; it never alters that boolean or subscription state. The version restriction remains intentional: fingerprints do not imply support for untested APK versions.

Runner usage, with the bundle and verified CLI JAR on the Java classpath:

```
dev.aurora.patches.RunnerKt list BUNDLE
dev.aurora.patches.RunnerKt apply BUNDLE INPUT_APK OUTPUT_APK NEW_WORK_DIRECTORY AAPT2 [PATCH_NAME ...]
```

Output and work paths must be new. Output is unsigned; the build entrypoint aligns, signs and verifies all four APKs with one private key. It does not install or publish anything. Use ADB install-multiple -r for an update signed with the same key.

The host code in this directory is GPL-3.0-only (see COPYING), matching its ReVanced dependencies. The injected runtime in `../src` and original theme assets remain MIT under the root LICENSE. No ReVanced host classes are injected into Spotify. No Eevee GPL code or assets were copied. Original Spotify binaries remain separately owned and are not published in the source repository.

References: https://github.com/ReVanced/revanced-patcher and https://github.com/ReVanced/revanced-cli/releases/tag/v6.0.0 .
