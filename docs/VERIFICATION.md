# Release 1 verification · 23 October 2026

Verdict: buildable review candidate; physical-device acceptance remains open. Current application code is commit `bf1afb0c64f98ec03ba07d694e3355adb161e498` on `codex/release-1-command-center`, in draft PR #1. Documentation/evidence changes do not add application functionality.

## Local execution

Java 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Android SDK 35 and explicitly pinned build-tools 35.0.0. SDK/dependencies were restored after recovering the review archive. No local physical phone or KVM device is available.

Executed:

```sh
gradle --no-daemon assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

The Gradle 8.9 distribution is the same version and checksum as the committed wrapper. The current environment needed its managed network proxy to download dependencies; no proxy or environment-specific trust settings were added to the repository.

| Check | Result |
| --- | --- |
| Final APK and instrumentation APK compilation | Passed; 76 tasks, 15 executed, 61 up to date, 54 seconds |
| Unit tests | 9 passed; zero failures, errors or skips |
| Lint | Zero errors; two GradleDependency notices for newer lifecycle libraries |
| APK signature | `apksigner verify` passed; standard debug certificate |
| Manifest permissions | RECORD_AUDIO, SET_ALARM and AndroidX-generated signature receiver permission; no INTERNET/AccessibilityService/background mic |
| Source secret-pattern check | No matching API-key/private-key patterns in app or workflow source; not an exhaustive secret audit |
| Whitespace validation | `git diff --check` passed |
| Runtime dependency report | Saved in verification/dependencies.txt; inventory, not a vulnerability-scan verdict |

The first complete build passed with no lint findings; a later metadata-aware run reported two dependency-update notices. They are preserved in `verification/lint.txt`. This is not a claim of an exhaustive dependency/security audit. A transient Kotlin compiler-daemon startup retry recovered and the build completed successfully.

Unit coverage: allowlisted app routes; bounded alarm/timer parsing and overflow handling; rejection of arbitrary prose; Maps destination handling; all eight reactor states and late-event rejection; explicit one-step plan/outcome semantics.

## GitHub Actions and emulator

Draft PR: https://github.com/Swapnild007/JAARVIS/pull/1
Current code run: https://github.com/Swapnild007/JAARVIS/actions/runs/39918527214

The first CI setup failed because setup-android requested the removed `tools` package. Fixed by naming the actual platform/build-tools packages.

An earlier API 35 x86_64 emulator run executed eight UI tests: seven passed and one exposed a real defect (the note keyboard hid the module Back control). Fixed by retaining the header on module screens while the keyboard is visible. Its original failure report was inspected; it is not counted as a passing run.

The build job passed. All 12 emulator tests passed (eight UI tests and four native-boundary tests), with zero failures/skips. The overall job still failed during screenshot collection because Gradle uninstalled the app after testing, removing its external files. The CI command now retains test APKs after the run, following the AndroidX project's own Gradle setting. A new run must verify collection; screenshot inspection remains pending until those images are actually retrieved.

Actual HTML runtime reports from the 12-test run are preserved under verification/emulator/. They establish API 35 emulator behavior for the current app code, not physical phone or audio behavior. Documentation-only commits are excluded from re-running identical application tests.

## Artifact identity

Delivered local debug APK: version `0.2.0-r1`, versionCode 2, package `com.jaarvis.commandcenter`, minSdk 26, targetSdk 35.

SHA-256: `33ed7900291fb33c5f2b1924f52dafd41152f03916cc2f7fc65d8c94b217795e`.

The old review APK and current APK use different debug signing certificates. The original private key was absent from the archive. Android may require uninstalling the old build; save notes first because uninstall clears local data. CI-generated debug APKs can also use a different certificate. No production signing key or credentials are committed.

## Remaining acceptance gates

Not verified on physical hardware: actual recognition/TTS audio and interruption; OS microphone-denial dialog; offline recognition/language availability; OEM app launches; saved Clock alarm/timer delivery; TalkBack; large font and orientation; frame timing and background power. See DEVICE_ACCEPTANCE.md.

The emulator's simulated voice-denial recovery test exercises the application's error path, not an actual operating-system permission dialog. Intent tests validate visible-review flags, URI encoding and honest unavailable-app results using a recording Context, not actual third-party app behavior.

General AI, API providers, autonomous tool orchestration, durable approved personal memory, wake word, background voice, incoming-call-style reminders, owned notification scheduling and remote engineering remain later releases. No overall project completion, merge or public deployment is claimed.

Original recovered reports remain under `verification/archive/` and do not verify this version.
