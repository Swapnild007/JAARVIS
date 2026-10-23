# Reference investigation

## Uploaded video

Inspected the supplied 22.13-second, 720 × 1280 video through decoded frames at 8 and 17 seconds. It shows a filmed desktop monitor with a luminous central reactor, orbiting roles, fine cyan circuit paths, side diagnostics and a microphone/status surface. The center changes between captured frames. This implementation adapts those observed elements to touch-first portrait proportions rather than reproducing a desktop layout at unreadable scale. Written specification takes precedence for cyan coloring and the eight named modules.

## Technical repository

Inspected https://github.com/AnaaySampat/jarvis at commit `923c549183190df248a737d43a9c452344d7f86f`.

- MIT license, copyright 2026 JARVIS contributors.
- React/Vite HUD → TypeScript routing → Tauri/Rust → Kotlin Android plugin.
- Dependencies include Tauri 2, React 18, native plugins and wake-word model assets.
- Reviewed `docs/ARCHITECTURE.md`, `package.json`, Android plugin paths and `RoutineAlarms.kt`. Its scheduling design distinguishes posting/requesting a reminder from actually executing a task; JAARVIS preserves that distinction in its Clock handoff.
- Reusable concepts: explicit native device boundary, local routing, separate speech adapters and scheduler seam.
- No application source or wake-word assets copied. JAARVIS is a new native Kotlin/Compose implementation with no React, Rust, remote operator, accessibility service or model runtime.
- Gradle wrapper launcher scripts/JAR copied from the reference's generated Android project. Original notices in scripts retained. MIT notice reproduced in `THIRD_PARTY_NOTICES.md`; Gradle wrapper itself is distributed under Apache 2.0.

## Initial repository state

JAARVIS was empty: no commits, source, workflows, tags or actual remote main ref. A local README seed commit establishes an ancestor for development. Publishing that seed to main was blocked by automatic approval review. No existing work was overwritten. SAARTHI was not accessed or modified.
