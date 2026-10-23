# JAARVIS · The Command Center

A native Android, local-first command center inspired by the supplied JARVIS video. Kotlin, Jetpack Compose and Canvas; no web runtime, backend, account or paid API key.

## Run

Requirements: Java 17, Android SDK platform 35 / build-tools 35.0.0, Android 8.0+ device (API 26+).

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.jaarvis.commandcenter/com.jaarvis.MainActivity
```

On Android: download the `jaarvis-debug-and-reports` artifact from a successful **Android verification** workflow run in [Actions](https://github.com/Swapnild007/JAARVIS/actions). Unzip it, open `app-debug.apk`, and grant your browser/files app permission to install this APK when Android asks. This is a debug build, not a signed production release. CI artifact availability depends on the workflow actually completing successfully.

## Use

Type a command or tap the central reactor / VOICE. Android requests the microphone only on demand. Tap again to cancel. A keyboard path remains available without microphone permission or a recognition service.

| Command | Native behavior |
| --- | --- |
| `time` / `what time is it` | Current device-local time and zone |
| `date` | Current date |
| `battery` | Android-reported level, or unavailable |
| `open settings` | Android Settings intent |
| `open youtube` | Installed YouTube launch, or actionable error |
| `open camera/maps/clock/calculator` | Request the corresponding installed app |
| `navigate to Pune` | Open a map query for review; no automatic route execution |
| `timer 5 minutes` | Clock timer review screen; maximum 24 hours |
| `cancel` / `stop` | Stop JAARVIS audio and pending work; external apps own dispatched requests |
| `alarm 07:30` / `set alarm at 7:30 pm` | Clock alarm screen; review and save there |

Alarm handoff is **not confirmation that an alarm was saved**. The Clock app owns the alarm and its behavior.

## Modules

- **Intelligence:** local grammar, latest 30 commands persisted locally, clear history.
- **Operations:** native device-action shortcuts.
- **Research:** explicit browser search handoff; no generated research results.
- **Memory:** one persistent local note; automatic save and clear.
- **Engineering:** supported command syntax and actual integration boundaries.
- **Finance:** decimal budget subtraction; no financial feeds or bank integration.
- **System:** device information, sound, motion, reduced motion, theme, permission-settings handoff.
- **Automations:** alarm/timer requests plus clear reminder limitations.

## Architecture

`core/Commands.kt` contains the platform-independent command parser, event-driven reactor state machine and interfaces for device actions, speech input/output and reminders. `platform/` implements these through Android APIs; `CommandCenterModel` owns the cancellable local operation, bounded history and persisted preferences. `ui/` renders the home graph and module screens. Android back and an explicit Back control return to the command center. Activity recreation preserves the module route and form state through saved state, with notes/preferences in SharedPreferences.

Reactor states follow real operation events: IDLE → LISTENING → THINKING → PLANNING → EXECUTING → SPEAKING → COMPLETED, with recoverable ERROR and cancellation. TTS starts the speaking state only through its real utterance callback. Speech recognition RMS drives listening pulse; TTS PCM amplitude is not exposed by this adapter, so speaking does not claim audio-amplitude visualization. Synchronous local actions can complete before an intermediate state is painted.

A single animation clock drives bounded Canvas drawing. It is removed when the app is not resumed, motion is zero, reduced motion is enabled, or Android animations are disabled. No endless background worker, wake lock or always-on microphone is used. Modern-device frame-rate and battery profiling still require physical hardware.

## Privacy and restrictions

No INTERNET permission, credentials or private signing key. Local notes and preferences are app-private; Android backup is disabled. The latest 30 command records persist in app-private storage and can be cleared from Intelligence. This is not an additional encrypted vault. Research explicitly sends the query to the selected external search site through a browser. Android's installed recognition/TTS providers may need their own network connection and downloaded voice data. Microphone denial and unavailable services are handled in the UI.

Not implemented: a cloud AI brain, provider configuration, incoming-call-style reminders, JAARVIS-owned notification scheduling, continuous wake word, screen-off listening, two-way voice reminders, app-screen automation, video calling or remote control. These are not represented as working integrations.

See [reference investigation](docs/REFERENCE.md), [verification evidence](docs/VERIFICATION.md), [next-session priorities](docs/NEXT_SESSION.md) and [third-party notices](THIRD_PARTY_NOTICES.md).

See [Recovery](docs/RECOVERY.md), [Architecture](docs/ARCHITECTURE.md), and [current verification](docs/VERIFICATION.md). Archived reports do not validate the current release.

If Android reports a signing conflict with the earlier review APK, save any needed notes first. The earlier debug signing key was not recoverable; uninstalling that build clears its local data. CI debug keys may also differ between builds. Production signing and upgrade continuity require a securely retained release key in a later release.
