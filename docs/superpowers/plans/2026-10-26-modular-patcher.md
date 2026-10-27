# Modular TV patcher implementation plan

**Goal:** Preserve the installed TV features while replacing the version-specific Python/smali rewriting path with runnable Kotlin patches using official ReVanced APIs.

**Architecture:** Keep the MIT Android runtime extension separate from GPL-3.0 build-host modules. Resolve bytecode targets by unique structural/string fingerprints, inject helper calls with ReVanced instruction APIs, and merge the precompiled extension DEX through the framework. Resource and raw asset changes are separate dependent patches. Signing and installing the complete four-APK set remain explicit build steps.

**Constraints:** No subscription changes. Preserve current app data by using the same signing key. No proprietary APKs or keys committed to Git. Compatibility metadata remains limited to the inspected package/version until other versions are tested; fingerprints do not imply universal compatibility.

**Accepted additions:** User approved default-on ad muting, then Spotify-style color accents/artwork blur, Upgrade hiding, polished UI, EQ/boost and a private GitHub release. User explicitly rejected shared-output effects. Audio effects therefore attach only to verified positive sessions owned by this process, using a one-time ADB diagnostic permission on the inspected Android 10 TV. The private release contains the four-APK installation set; source Git remains free of proprietary APKs and keys.

- [x] Install and verify the current lyrics build on the connected TV.
- [x] Pin a working official ReVanced Patcher/CLI/compiler combination and record provenance/licenses.
- [x] Add Kotlin patch modules and a reusable unique-match layer with missing/ambiguous/idempotency checks.
- [x] Cover extension merging, lifecycle/remote key hooks, WebView completion and own-session capture; separate manifest/assets patches with dependencies.
- [x] Add a runnable build entrypoint, bundle discovery, patch listing and selected-patch execution.
- [x] Test structural match behavior, renamed obfuscated class compatibility, invalid matches, and repeated application. Apply to the user's original APK and verify generated call sites and embedded classes/assets.
- [x] Align/sign all four APKs, verify signatures, install and smoke-test the migrated build; preserve login/settings.
- [x] Review, update source-only CI and docs, push to the existing private GitHub repo, and refresh local artifacts.

Review focus: reject ambiguous fingerprints, avoid changing register meaning, reject duplicate injection, use the current API rather than outdated context types, and keep GPL build-host code outside the injected MIT extension.
