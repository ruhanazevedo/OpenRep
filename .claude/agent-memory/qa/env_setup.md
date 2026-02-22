---
name: workout-generator env setup
description: Environment setup for workout-generator Android project — no .env required
type: project
---

This is a pure Android/Kotlin project. No .env file is needed for code review or static analysis.

- No backend server, no API keys required for build analysis
- Build system: Gradle with Kotlin DSL (build.gradle.kts)
- Dependency catalog: gradle/libs.versions.toml
- Testing: Robolectric unit tests, no instrumentation tests needed for QA review sessions

**Why:** Project is self-contained Android app with Room DB; YouTube API key is optional (loaded from gradle.properties, defaults to empty string).

**How to apply:** Jump straight to reading source files. No setup phase needed for code review tasks.
