---
name: Generate screen transition — transparent background bug
description: Library content bleeds through during slide transition to GenerateScreen because it has no opaque background container
type: project
---

GenerateScreen (GenerateScreen.kt) is the only top-level bottom-nav destination that lacks a Scaffold or Surface. During NavHost slide transitions (slideInHorizontally), the entering GenerateScreen Column is transparent, causing the exiting LibraryScreen to show through.

**Why:** Every other top-level screen (LibraryScreen, GenerateFilterScreen, HistoryScreen, SettingsScreen) uses Scaffold as its root container, which provides an opaque surface. GenerateScreen uses a bare Column.

**How to apply:** When reviewing or testing Generate-related screens, remember this screen intentionally needs an opaque root container added. Fix is: add Surface(fillMaxSize) wrapper or background modifier to GenerateScreen's Column.

Key files:
- /root/repos/workout-generator/app/src/main/java/com/ruhanazevedo/openrep/ui/screens/GenerateScreen.kt (line 22 — bare Column, no background)
- /root/repos/workout-generator/app/src/main/java/com/ruhanazevedo/openrep/WorkoutGeneratorNavHost.kt (lines 92-99 — slide transitions defined)
