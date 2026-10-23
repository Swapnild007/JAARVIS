# Native JAARVIS command center with local Android commands

JAARVIS was empty. This change adds a native Kotlin/Compose Android application whose animated central reactor connects eight working module routes. Local commands read the real device clock/date/battery and hand off Settings, installed YouTube and alarm requests to supported Android APIs. Voice adapters use on-demand SpeechRecognizer and TextToSpeech; failures remain recoverable through typed input.

## Implementation

- Compose Canvas reactor, bounded orbital drawing, real event-driven operation states, lifecycle/reduced-motion controls.
- Eight modules: local command history, device actions, external browser search, persistent note, engineering help, decimal budget calculator, real device information/preferences, Clock alarm handoff.
- Typed input, runtime microphone request, cancellation, speech-service availability/errors, TTS utterance callbacks.
- Saved sound/motion/theme preferences, local note storage, safe drawing insets, keyboard-aware command input, touch and TalkBack labels.
- Core interfaces for commands, speech, actions and reminder scheduling; no backend or paid API dependency.
- Debug-APK artifact workflow and API 35 instrumentation job. Reference inspection and third-party attribution documented.

## Verification

See [VERIFICATION.md](VERIFICATION.md) for exact final commands, outcomes and runtime limitations. Five unit tests cover command routing and state transitions. Screenshots are included only if captured from an actual running app; no generated mockup is presented as app evidence.

## Install

Use the successful Android verification workflow's `jaarvis-debug-and-reports` artifact, extract `app-debug.apk` and install on Android 8+. Or run `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Limitations

No physical-phone verification. Speech availability depends on installed Android recognition/TTS providers and voice data. Clock opens for user review: JAARVIS does not claim the alarm was saved. No cloud AI, call-style reminders, continuous wake word, screen-off listening, JAARVIS-owned scheduled notifications, video calling or arbitrary app-screen automation.

## Next session

Physical OPPO voice, permission, keyboard, gesture, rotation, font-size and performance checks; API 35 instrumentation; persistent notification reminders with Android restrictions handled explicitly. See [NEXT_SESSION.md](NEXT_SESSION.md).

Do not merge or deploy. If main is absent, publishing the seed README to establish a PR base requires approval; the application code stays on `feat/jaarvis-command-center`.
