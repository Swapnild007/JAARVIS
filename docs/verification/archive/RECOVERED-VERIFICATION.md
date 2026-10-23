# Verification evidence · 23 October 2026

## Environment

Java 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Android SDK 35 / build-tools 35.0.0. No physical Android device and no `/dev/kvm` hardware acceleration. AOSP API 29 x86_64 software emulator was installed for an attempted runtime check. API 35 instrumentation is configured separately in CI.

## Build and unit checks

```sh
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

The first complete check found one lint error: an API 27 navigation-bar style attribute with minimum SDK 26. Removed the attribute; edge-to-edge system bar appearance is handled through Activity APIs. Added explicit backup/data-transfer exclusions for Android 12+ and disabled legacy backups.

Final local wrapper command completed successfully in 1m 58s: 76 tasks, 42 executed and 34 up to date. Five unit tests passed (zero failures/errors/skips). Final lint report: **No issues found**. The application and instrumentation APKs were generated. The reports persisted with the workspace and were read again after the session resumed.

The resumed execution environment had been reset: `/tmp` toolchains, logs and emulator processes no longer existed. The unfinished software-emulator run therefore provides no successful runtime evidence. CI is used for subsequent runtime verification and real app screenshots. Additional screenshot-capture instrumentation added after resumption must pass CI before being described as verified.

Unit coverage: all six device-command routes; 24-hour and AM/PM alarms including noon/midnight; invalid times; arbitrary text not interpreted as actions; operation state transitions; cancel/reset and late-event rejection.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. APK signature verification through `apksigner verify` passed. No private signing key is tracked; Android generated its normal local debug key outside this repository.

## Runtime evidence

No successful runtime result was retained. Source-code existence is not runtime proof. The first SwiftShader software-rendering attempt booted and installed the app, but screenshot capture stalled after its PNG header and is not presented as a successful screenshot. A second software-only emulator attempt was interrupted by the environment reset before its result could be collected.

The instrumentation APK includes tests for all eight module controls and Back navigation, typed battery command, persistent-note controls, reduced-motion settings, and recoverable unknown commands. Compiling these tests does not establish that they passed at runtime.

## Physical-device release gates

Not verified on a real phone: microphone grant/deny with an installed recognizer; recognition and speech audio; TTS language/voice installation; YouTube launch when present; Clock app saved-alarm delivery; TalkBack; OEM keyboard; cutouts and gesture navigation; rotation and large font; frame-time and background power.

No call-like reminders, screen-off microphone, continuous wake word or two-way voice reminder are implemented. Clock handoff is not confirmation of a saved alarm.

## Repository and PR gate

Initial remote repository had no commits or branches. The local seed README commit is `5338939`. Automatic approval review rejected publishing that seed to main because direct default-branch initialization was not specifically authorized. No workaround is used. The requested feature branch holds all application work locally. Publishing that branch failed after resumption because the Git transport had no authentication (`could not read Username`). The connected GitHub API confirms that the remote still has no refs (409: Git Repository is empty). No feature branch or CI run has been published. A draft PR into main requires a real main ref and may remain blocked until initialization is approved. Do not merge or deploy.

## Delivery verdict

**Buildable review candidate; release acceptance incomplete.** The app builds, all five unit tests pass, lint is clean, and the debug APK was signature-verified. Rendering, animation, eight-module navigation, platform actions, permissions and actual speech/TTS have not been established by a completed runtime test. No screenshot is claimed. API 35 UI tests and real-phone checks remain required.

The review ZIP contains the actual debug APK, source archive and the saved unit/lint reports. Its source includes the CI screenshot-capture test added after the last local compilation; that additional test remains uncompiled until CI or the Android toolchain is restored. The application Kotlin source in the APK is unchanged from the successful local wrapper build.

To unblock GitHub delivery, approve creating `main` with the seed README only. Then the connected GitHub tools can publish the feature implementation and create the draft PR. This does not merge or deploy the application.
