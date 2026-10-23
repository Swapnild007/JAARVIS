# Release 1 acceptance first

P0: Complete remaining API 35 CI checks, inspect screenshots, fix any failures in the existing draft PR #1. Do not open another PR, merge, or deploy.

P0: Perform the physical-device checklist in DEVICE_ACCEPTANCE.md. Capture actual voice input/output, microphone denial, OEM app availability and Clock delivery evidence. Inspect motion, TalkBack, large text, orientation, and background battery use. Emulator evidence is separate from physical-device evidence.

P1: Decide a securely retained signing identity for update continuity before any production release. The old review debug key is unavailable; do not pretend debug builds can always upgrade each other. Review dependency version alignment; lint may report newer lifecycle libraries. Do not blindly upgrade an entire toolchain during R1 validation.

After Release 1 acceptance, begin Release 2: one unified orchestration core, user-configured AI providers, encrypted credential storage, explicit personal-memory approval/export/delete and real tool execution. Keep deterministic Android commands available without a provider. Follow ARCHITECTURE.md for later release boundaries.
