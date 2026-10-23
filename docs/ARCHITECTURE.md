# Architecture decision: preserve the recovered Android application

Status: accepted for Release 1, 23 October 2026.

## Evidence and decision

The remote JAARVIS repository was empty at session start. The review archive contained a complete Kotlin/Compose application, wrapper, tests, reports and an APK. Preserve that source as `codex/recovered-baseline`; implement incrementally on `codex/release-1-command-center`. Do not migrate to a second UI runtime or initialize main.

The technical reference `AnaaySampat/jarvis` was inspected at `923c549183190df248a737d43a9c452344d7f86f`: README, architecture/security/status documentation, package manifest, Kotlin accessibility/wake-word plugin paths, and MIT license. It uses React → TypeScript brain → Tauri/Rust → Kotlin. Its README labels it experimental and tested on its author's Samsung phone. Its operator, providers, wake word and remote control are not JAARVIS capabilities. They have not been executed or independently validated here.

Native Compose reuses the recovered functional source, avoids Rust/WebView bridge migration, and exposes Android lifecycle/permissions directly. The cost is that future non-Android clients must implement their own UI and adapters. The platform-independent command/plan/result contracts are the starting boundary; Android is not intended to define the future intelligence architecture.

## Runtime boundaries

| Layer | Responsibility |
| --- | --- |
| Compose UI | Eight real module routes, reactor, keyboard, on-demand microphone permission |
| CommandCenterModel | One cancellable local operation, generation tokens, observable state, bounded history |
| core | Allowlisted grammar → one-step CommandPlan → DeviceActions → typed Outcome |
| platform | Android intents, battery/time readings, speech/TTS, app-private storage |

The eight reactor states represent actual events. Parsing and bounded plan construction run before dispatch, with cooperative cancellation points. They may be too quick to paint separately; there are no artificial thinking delays. COMPLETED means JAARVIS completed its own operation. A HANDOFF result explicitly does not verify another app's result. The persistent execution trace records command stages and the adapter outcome, not private speech recordings or invented tool evidence.

The app has no INTERNET permission, remote tool runtime, cloud credentials, embedded browser, AccessibilityService or autonomous messaging. Browser queries use a user-triggered external intent. App intents are created by native allowlisted code, never by model-generated schemes. Maps destinations are URI-encoded data. Camera opens the camera app; it does not capture a photograph automatically.

## Voice and lifecycle

Push-to-talk requests RECORD_AUDIO only on activation. API 31+ prefers Android's on-device recognizer when available; otherwise the installed system recognizer may send audio to its service. A 20-second watchdog exits a stalled recognition session. TTS has a 45-second watchdog; starts/finishes follow real engine callbacks. Generation tokens reject obsolete speech/TTS results. STOP cancels JAARVIS work and audio; it cannot undo an intent already delivered to another app.

On background entry the app stops microphone, TTS and local operations. Configuration changes retain the ViewModel. Reactor motion is gated by resumed lifecycle, reduced-motion preference and system animation settings. Large font scale uses separate readable module controls. No foreground service, wake lock, screen-off microphone or boot receiver is used.

## Storage

Preferences, editable note and the latest 30 command records live in app-private SharedPreferences. This is sandboxed storage, not an additional encrypted vault. Android backup and device-transfer extraction are disabled. Notes can be edited or cleared; command history can be reviewed and cleared. Uninstall removes local data. No personal facts are inferred into durable memory. Provider credentials and durable personal-memory approval/export workflows belong to Release 2.

## Speech research for Release 3

- Android recognizer API: https://developer.android.com/reference/android/speech/SpeechRecognizer
- sherpa-onnx Android support: https://k2-fsa.github.io/sherpa/onnx/android/index.html
- sherpa-onnx runtime (Apache-2.0): https://github.com/k2-fsa/sherpa-onnx
- Whisper ONNX export and model variants: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/whisper/export-onnx.html
- Candidate model card: https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny.en
- Original Whisper code/weights license: https://github.com/openai/whisper
- Foreground microphone restrictions: https://developer.android.com/develop/background-work/services/fgs/service-types

Evaluation: sherpa-onnx offers Android ASR, VAD and keyword spotting without runtime network dependency, but adds native binaries and model downloads. A model's redistribution terms must be checked separately from the runtime license, with checksums, memory/latency/power measurements and language accuracy on the target phone before selection. No offline model or wake-word engine is bundled in R1. A foreground microphone service would need visible notification, explicit user start, STOP, battery testing and compliance with Android while-in-use restrictions. No continuous hands-free claim is made.

## Next releases

R2: provider interfaces, encrypted credentials, unified tool orchestration and user-approved memory. R3: advanced voice, owned reminders, permission-aware operator. R4: research/engineering execution and GitHub. R5: dependency-aware strategy and cross-system automation. Release 1 acceptance precedes this expansion.
