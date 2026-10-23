# Next session

## P0 · Release gate

1. Establish main as a PR base only with explicit approval if it is still absent; open the prepared draft PR. Do not merge or deploy.
2. Install CI-built debug APK on the user's physical OPPO phone. Validate microphone denial/grant, installed recognizer, TTS engine/language, device-local clock, YouTube presence, Clock confirmation, keyboard, TalkBack, rotation, large font and navigation gestures.
3. Profile actual animation frame time and background power. Software-emulator timing does not establish phone performance.
4. Run the instrumented suite on API 35 through GitHub Actions. Physical phone success and API 29 emulator success are separate evidence.

## P1 · Reminder delivery

Add JAARVIS-owned local notification reminders with notification permission handling, persistent schedules, reboot restore, timezone changes, cancellation and Doze tests. Choose exact alarms only when warranted and authorized; handle Android exact-alarm access restrictions. Do not describe Clock intent requests as verified reminders.

## P2 · Voice extension

Add secure cloud-provider options only as a separately scoped capability. Research wake-word, screen-off audio and call-like reminders against Android foreground-service, microphone, notification and full-screen intent restrictions. Specify and test lifecycle, privacy and power costs before exposing controls.
