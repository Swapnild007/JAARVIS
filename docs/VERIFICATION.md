# Cinematic revision 03 verification · 23 October 2026

Application source: `162ce9b9fdd4b4ebed7498fa3673cacb9ba7ea89` on `codex/release-1-command-center`, [draft PR #1](https://github.com/Swapnild007/JAARVIS/pull/1). Documentation/evidence commits do not change this application source. See [root cause and design comparison](CINEMATIC_UI.md).

## Build and automated checks

Java 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Android SDK 35 / build-tools 35.0.0; minSdk 26. No dependency versions changed for the cinematic revision.

```sh
./gradlew -Pkotlin.compiler.execution.strategy=in-process --no-daemon clean assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

Local clean build passed in 43 seconds, 77 tasks executed. An incremental build first encountered stale duplicate generated instrumentation R-class dex entries; a clean rebuild regenerated the outputs and passed. The final local build used the already downloaded dependency cache in offline mode. No proxy or trust-store settings were added to the repository.

Local unit tests: **9 passed**, zero failures/errors/skips. Final clean local lint: **zero findings**; earlier metadata-aware incremental lint reported three dependency-version update notices. These are not security findings or an exhaustive dependency audit.

Passed CI run: [39930967655](https://github.com/Swapnild007/JAARVIS/actions/runs/39930967655). Both jobs passed. API 35 Pixel 6, 1080×2400 portrait: **15 tests passed** (8 UI, 4 native-boundary, 3 reactor rendering). A second run at 720×1280, density 320 and 150% text passed **all 8 UI tests**. Zero failures/skips. The portrait overlap assertion passed. All eight module destination captures were retrieved; home, enlarged-text, module and state fixtures were inspected.

CI lint completed with **zero errors and 6 warnings**: one OldTargetApi (SDK 35) and five GradleDependency update notices. The online CI report is authoritative for metadata-dependent checks; local offline lint had no findings. The unchanged toolchain remains pinned for this visual milestone; latest-SDK/dependency migration needs its own compatibility testing. See `verification/cinematic/ci-lint.html`.

- [APK, unit and lint artifact](https://github.com/Swapnild007/JAARVIS/actions/runs/39930967655/artifacts/12491697381)
- [Original PNG screenshots and emulator reports](https://github.com/Swapnild007/JAARVIS/actions/runs/39930967655/artifacts/12490984894)
- [Before/after image](verification/cinematic/before-after.jpg), same emulator viewport; baseline run 39921509671.
- [Current portrait home](verification/cinematic/default/home.jpg) and [150% text home](verification/cinematic/large-text/home.jpg). Enlarged text intentionally scrolls the main content while retaining the command dock. These committed JPEGs are resized review copies; full PNGs and all eight state fixtures are in the artifact.

Delivered APK SHA-256: `2ae0be1793771a0e761e1bc8364a29a993ff0c39ba6eee242d665c1ea4c8e6dd`. `apksigner verify` passed; certificate SHA-256 `4f338b341095000c3c046a4d9c80047080fc866821dde7714810f7a7517fe4d8`. CI build and emulator jobs independently build the same source; the downloadable APK comes from the build job. Manifest inspection confirms the Cinematic package/label and only RECORD_AUDIO, SET_ALARM and the AndroidX-generated signature receiver permission. No INTERNET, AccessibilityService or background-microphone permission was added.

## Regression coverage

Existing parser, state-machine, typed commands, error/reset, cancellation, persistence/recreation, note/preferences, all eight module routes and native intent boundary tests remain. No changes were made to core commands, Android adapters or CommandCenterModel.

Three rendering tests cover all eight reactor states and subsystem callbacks, pixel changes with orbital animation, and stable pixels with reduced motion. State fixtures directly set renderer inputs: they do **not** demonstrate real recognition, TTS audio or task execution. The default-portrait UI test also checks every subsystem touch target is at least 48dp high and fully above the fixed command dock.

## Evidence scope

Emulator evidence is distinct from physical-phone acceptance. Android recognition/TTS services were unavailable on the emulator; their unavailable states are visible and honest. Real microphone recognition, spoken interruption, OEM app behavior, saved Clock alarms/timers, TalkBack and physical frame/battery profiling still require the [device checklist](DEVICE_ACCEPTANCE.md).

No continuous wake word, background microphone, new AI provider, remote execution or reminder service was added. No overall intelligence completion, merge or public deployment is claimed.

Previous basic-UI evidence is archived under `verification/pre-cinematic/` and `verification/archive/`; it does not validate revision 03. Actual emulator screenshots are captures of the installed Compose application, not generated mockups. CI disables OS animations for ordinary UI tests; the dedicated Canvas motion test explicitly advances its own Compose animation clock.

## Safe install

Debug package `com.jaarvis.commandcenter.cinematic`, label **JAARVIS Cinematic**, version `0.3.0-cinematic` / code 3. This deliberately installs alongside the old package, preserving that app and its data. **Do not uninstall or clear the old app.** The new package starts with separate storage. See [signing and migration limitations](CINEMATIC_UI.md#safe-installation-and-signing).
