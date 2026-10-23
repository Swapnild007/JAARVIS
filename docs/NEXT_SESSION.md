# Release 1 acceptance first

P0: Review the cinematic revision 03 before/after evidence and APK in existing draft PR #1. Preserve this visual milestone and its parser/action/voice regression checks. Do not open another PR, merge, or deploy. Check docs/VERIFICATION.md for the tested source SHA and evidence rather than inferring success from a screenshot alone.

P0: Perform the physical-device checklist in DEVICE_ACCEPTANCE.md. Capture actual voice input/output, microphone denial, OEM app availability and Clock delivery evidence. Inspect motion, TalkBack, large text, orientation, and background battery use. Emulator evidence is separate from physical-device evidence.

P1: Decide a securely retained signing identity for update continuity before any production release. The old review debug key is unavailable. Cinematic installs side by side as com.jaarvis.commandcenter.cinematic; preserve the old app/data and do not pretend debug builds can always upgrade each other. Review dependency version alignment; lint may report newer lifecycle libraries. Do not blindly upgrade an entire toolchain during R1 validation.

After Release 1 acceptance, begin Release 2: one unified orchestration core, user-configured AI providers, encrypted credential storage, explicit personal-memory approval/export/delete and real tool execution. Keep deterministic Android commands available without a provider. Follow ARCHITECTURE.md for later release boundaries.
