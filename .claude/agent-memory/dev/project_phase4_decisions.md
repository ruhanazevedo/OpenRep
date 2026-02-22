---
name: Phase 4 implementation decisions
description: Key non-obvious decisions made during Phase 4 (issues #16–19) on 2026-03-31
type: project
---

Phase 4 implementation completed 2026-03-31.

**Why:** Phase 4 adds YouTube linking, active sessions, session history, and UI polish.

**How to apply:** Reference these when working on related features.

## #16 — YouTube search

- `extractYouTubeId` already existed as a top-level function in `LibraryViewModel.kt`. The duplicate in `SearchYouTubeViewModel` was renamed to `parseYouTubeId` (private) to avoid compile clash.
- `NetworkModule` provides Moshi, OkHttpClient, and YouTubeApiService as singletons via Hilt. Base URL: `https://www.googleapis.com/youtube/v3/`.
- API key gated via `BuildConfig.YOUTUBE_API_KEY` — search UI hidden if blank, paste-URL path always visible.
- `SearchYouTube` route added to `Screen.kt` as `search_youtube/{exerciseId}`.
- ExerciseDetailScreen gained `onSearchYouTube` callback + VideoLibrary icon (visible for all exercises, not just custom ones).

## #17 — Active session

- `SessionViewModel` creates a `WorkoutSessionEntity` row on `init` (not on button press) to guarantee the row exists before any sets are logged.
- Rest timer uses a `viewModelScope` coroutine with `delay(1_000)` loop; `restTimerJob` is cancelled on skip and on `finishWorkout`.
- Exercises are flattened from all days and sorted by `dayIndex` then `orderIndex` — single scrollable list, not day-by-day.
- "Finish Workout Early" button always visible; normal finish shows after all sets are done.

## #18 — Session history

- `HistoryViewModel` rewritten to emit `HistoryUiState` containing both plans and `List<SessionSummary>`. Uses `combine` + `flatMapLatest` + `flow {}` to build set counts via `SessionSetDao.countBySessionId` (new suspend query).
- `WorkoutPlanExerciseDao.getById(id: String)` added as a new suspend query for `SessionDetailViewModel` to resolve `planExerciseId → exerciseId`.
- `SessionDetailScreen` groups sets by `planExerciseId`, resolves exercise names via the DAO chain.
- `HistoryScreen.onSessionClick` renamed to `onPlanClick` / `onSessionDetailClick` at call site in NavHost.

## #19 — UI polish

- Screen transitions: `slideInHorizontally { it }` forward, `slideOutHorizontally { -it/3 }` exit (parallax feel), reversed for pop.
- Shimmer: `ShimmerBox` / `ShimmerList` in `ui/components/ShimmerPlaceholder.kt` using `rememberInfiniteTransition` alpha 0.3→1.0.
- `animateItem()` (Compose 1.7+ API, available via BOM 2024.11.00) used on LazyColumn items in LibraryScreen.
- History badge: `HistoryViewModel` hoisted to NavHost scope to count plans for `BadgedBox`.
- `CircularProgressIndicator` replaced with shimmer in LibraryScreen and PlanDetailScreen loading states.
