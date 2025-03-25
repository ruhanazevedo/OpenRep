# PRD — Workout Generator (Android)

**Version:** 1.0
**Date:** 2026-03-27
**Status:** Draft

---

## 1. Overview

Workout Generator is a native Android application that generates personalized weekly workout plans based on user-defined filters. The app operates entirely offline — no account, no remote database, no cloud sync. All data (exercise library, workout history, preferences) lives on-device via SQLite/Room.

The primary workflow is filter-first: the user sets training parameters, taps generate, reviews the resulting plan, optionally swaps exercises, and starts the session. YouTube video demos are linked per exercise to guide correct form.

---

## 2. Goals

- Let any user — regardless of training experience — produce a structured, sensible weekly workout plan in under 60 seconds.
- Ship a curated local exercise library that covers all major muscle groups, equipment tiers, and difficulty levels.
- Allow power users to extend the library manually or via JSON import.
- Surface YouTube form demos inline so users never have to leave the app mid-workout.
- Keep the app 100% functional with no internet connection (except video playback).

---

## 3. Non-Goals

- No remote database or backend server — ever.
- No user accounts, login, or cloud sync.
- No social features (sharing plans, leaderboards, communities).
- No iOS version in this scope.
- No AI/LLM-based plan generation — logic is rule-based.
- No nutrition tracking, calorie counting, or diet planning.
- No wearable / Bluetooth device integration.
- No paid subscription or in-app purchase in v1.

---

## 4. User Stories

### Workout Generation
- As a user, I want to choose how many days per week I can train so the plan fits my schedule.
- As a user, I want to select which muscle groups to target so the plan reflects my goals.
- As a user, I want to choose a training split type so the weekly structure matches how I prefer to train.
- As a user, I want to generate a complete weekly plan in one tap so I don't have to build it manually.
- As a user, I want to swap any exercise within a generated plan so I can work around equipment or personal preference.

### Exercise Library
- As a user, I want to browse all exercises in the library so I know what's available.
- As a user, I want to add my own exercises so I can use movements not in the default library.
- As a user, I want to import exercises from a JSON file so I can bulk-add a custom library.
- As a user, I want to see details for each exercise (muscle groups, equipment, difficulty, instructions) so I know how to perform it correctly.

### YouTube Integration
- As a user, I want to watch a short video demo for any exercise inline so I don't have to context-switch to a browser.
- As a user, I want to link a YouTube video to any exercise I add so my custom exercises also have demos.
- As a user, I want to search YouTube to find and attach a video to an exercise from within the app.

### Workout Session
- As a user, I want to start a workout session from a generated plan so I can track progress set by set.
- As a user, I want to log completed sets and reps so I can review what I did.
- As a user, I want my workout history saved locally so I can track progress over time.

---

## 5. Feature Specifications

### 5.1 Workout Generation

#### 5.1.1 Filters

The user configures the following before generating:

| Filter | Type | Values |
|--------|------|--------|
| Days per week | Integer selector | 1 – 7 |
| Muscle groups | Multi-select | Chest, Back, Shoulders, Biceps, Triceps, Legs (Quads, Hamstrings, Glutes, Calves), Core, Full Body |
| Training split | Single-select | A, AB, ABC, PPL, AA |

**Split definitions:**

- **A** — Full-body every session. All selected muscle groups hit every training day.
- **AB** — Two alternating sessions. Day A and Day B each hit a subset of the selected muscle groups.
- **ABC** — Three alternating sessions. Muscle groups distributed across Day A, B, and C.
- **PPL** — Push / Pull / Legs structure. Selected muscle groups mapped to push (chest, shoulders, triceps), pull (back, biceps), and legs (quads, hamstrings, glutes, calves).
- **AA** — Same workout repeated. Identical session replicated across all training days.

#### 5.1.2 Generation Logic

1. Map selected muscle groups to the split structure.
2. For each muscle group in each day, query the local exercise library filtered by: muscle group match, user-specified equipment availability (if set in preferences), and difficulty range (if set in preferences).
3. Select a configurable number of exercises per muscle group per session (default: 3–4).
4. Assign default sets/reps scheme based on difficulty and split type.
5. Output a `WorkoutPlan` object: N sessions, each with an ordered exercise list and set/rep targets.

#### 5.1.3 Plan View and Editing

- Display the generated plan as a scrollable list: Day → Sessions → Exercises.
- Each exercise card shows: name, target muscle, sets × reps, and a thumbnail/play button if a YouTube video is linked.
- "Swap" action on any exercise opens a filtered list of alternative exercises for the same muscle group; selecting one replaces it in the plan.
- "Regenerate" re-runs the generation logic with the same filters.
- "Save plan" persists the plan to the local database for future use.

---

### 5.2 Exercise Library

#### 5.2.1 Data Schema (per exercise)

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key, auto-generated |
| `name` | String | Required, unique |
| `muscle_groups` | List\<String\> | At least one required; drawn from the canonical set |
| `secondary_muscle_groups` | List\<String\> | Optional |
| `equipment` | String | Barbell, Dumbbell, Cable, Machine, Bodyweight, Kettlebell, Resistance Band, Other |
| `difficulty` | Enum | Beginner / Intermediate / Advanced |
| `instructions` | String | Free-text, step-by-step form cues |
| `youtube_video_id` | String | YouTube video ID (11 chars); nullable |
| `is_custom` | Boolean | True if user-added; false for seeded library entries |
| `created_at` | Timestamp | Auto-set on insert |

#### 5.2.2 Seeded Library

The app ships with a curated exercise library covering all canonical muscle groups across all equipment tiers. Seeded entries have `is_custom = false` and cannot be deleted (only hidden). Seeded data is bundled as a Room prepopulated database asset or a migration seed script.

Minimum viable seed: ≥ 5 exercises per major muscle group, covering at least 3 equipment types.

#### 5.2.3 Manual Add

- A form accessible from the library screen lets users create a new exercise.
- All fields except `secondary_muscle_groups`, `instructions`, and `youtube_video_id` are required to save.
- On save, `is_custom` is set to true.

#### 5.2.4 JSON Import

**Import format:**

```json
[
  {
    "name": "Bulgarian Split Squat",
    "muscle_groups": ["Quads", "Glutes"],
    "secondary_muscle_groups": ["Hamstrings", "Core"],
    "equipment": "Dumbbell",
    "difficulty": "Intermediate",
    "instructions": "Stand facing away from a bench...",
    "youtube_video_id": "dQw4w9WgXcQ"
  }
]
```

- Import is triggered from the library screen ("Import from file").
- The user selects a `.json` file via the system file picker (Storage Access Framework).
- The app validates each entry: required fields present, `muscle_groups` values within canonical set, `difficulty` within allowed enum.
- Valid entries are inserted; invalid entries are skipped with a per-entry error message shown in a summary dialog.
- Duplicate names (case-insensitive) prompt the user: skip or overwrite.

#### 5.2.5 Edit and Delete

- User-added (`is_custom = true`) exercises can be edited or deleted.
- Seeded exercises can be edited for personal notes/instructions only; core fields (name, muscle groups, equipment, difficulty) are read-only.
- Deleting an exercise that appears in a saved workout plan: mark as deleted in the library, replace the reference in plans with a "deleted exercise" placeholder rather than cascading delete.

---

### 5.3 YouTube Integration

#### 5.3.1 Linking a Video

- From the exercise detail screen, users tap "Link YouTube video".
- A search screen opens: user types a query, app calls the YouTube Data API v3 (search endpoint) and displays results (title, thumbnail, channel, duration).
- User taps a result to preview it inline; confirms to save the `youtube_video_id` to the exercise record.
- Alternatively, user can paste a YouTube URL directly; the app extracts the video ID.

#### 5.3.2 Video Playback

- When a video is linked, an inline player (YouTube Android Player API or a WebView with the YouTube IFrame Player) is shown on the exercise detail screen.
- Autoplay is off by default; user taps play.
- If offline, the player shows a "Video unavailable offline" message rather than crashing.

#### 5.3.3 API Key Management

- YouTube Data API v3 key is stored in `local.properties` (not committed to source control) and referenced via `BuildConfig`.
- If the API key is absent or quota is exhausted, the search feature degrades gracefully: user can still link by pasting a URL directly.

---

### 5.4 Workout Session

#### 5.4.1 Starting a Session

- "Start workout" on any saved or generated plan opens the active session screen.
- The session presents exercises sequentially, one at a time, with set/rep targets.

#### 5.4.2 Logging

- For each set: user logs actual reps completed and optional weight.
- "Done" marks the set complete and advances to the next.
- A rest timer (configurable in preferences, default 60 s) counts down between sets.

#### 5.4.3 History

- Completed sessions are persisted to the `workout_history` table.
- History screen shows a calendar-style overview and a list of past sessions.
- Tapping a session shows the full exercise/set/rep/weight log.

---

### 5.5 User Preferences

Stored locally in Room or SharedPreferences:

| Setting | Default |
|---------|---------|
| Default rest timer (seconds) | 60 |
| Equipment available | All |
| Preferred difficulty range | Beginner–Advanced |
| Exercises per muscle group (generation) | 3 |

---

## 6. Data Model Outline

```
┌─────────────────────────────────────────────────────────┐
│  exercises                                              │
│  id (UUID PK) · name · muscle_groups · secondary_       │
│  muscle_groups · equipment · difficulty · instructions  │
│  youtube_video_id · is_custom · is_deleted · created_at │
└──────────────────────────┬──────────────────────────────┘
                           │  referenced by
          ┌────────────────▼─────────────────┐
          │  workout_plan_exercises           │
          │  id · plan_id (FK) · exercise_id  │
          │  (FK) · day_index · order_index   │
          │  sets · reps · notes             │
          └────────────────┬─────────────────┘
                           │  belongs to
          ┌────────────────▼─────────────────┐
          │  workout_plans                   │
          │  id · name · split_type ·        │
          │  days_per_week · muscle_groups   │
          │  created_at · is_template        │
          └────────────────┬─────────────────┘
                           │  instantiated as
          ┌────────────────▼─────────────────┐
          │  workout_sessions                │
          │  id · plan_id (FK) · started_at  │
          │  completed_at · notes            │
          └────────────────┬─────────────────┘
                           │  contains
          ┌────────────────▼─────────────────┐
          │  session_sets                    │
          │  id · session_id (FK) ·          │
          │  plan_exercise_id (FK) ·         │
          │  set_number · reps_completed ·   │
          │  weight_kg · completed_at        │
          └──────────────────────────────────┘

  user_preferences (key-value or typed singleton table)
```

All foreign keys are enforced by Room. No remote tables.

---

## 7. Tech Stack Recommendation

| Layer | Choice | Rationale |
|-------|--------|-----------|
| Language | Kotlin | First-class Android support, modern idioms |
| UI | Jetpack Compose | Declarative, no XML layouts, easier to maintain |
| Navigation | Navigation Compose | Native Compose nav graph |
| Local DB | Room (SQLite) | Type-safe ORM, migrations, coroutine support |
| Async | Kotlin Coroutines + Flow | Room emits Flows; fits Compose state model |
| DI | Hilt | Compile-time safe, Android-aware |
| YouTube playback | YouTube Android Player API or WebView IFrame | Player API is smoother; WebView is fallback if API quota/key issues |
| YouTube search | YouTube Data API v3 (Retrofit + Moshi) | Standard REST; key in `local.properties` |
| Image loading | Coil | Compose-native, lightweight |
| Testing | JUnit 4 + Espresso + Robolectric | Room in-memory DB for unit tests; Espresso for UI flows |
| Build | Gradle (Kotlin DSL) | Consistent with modern Android projects |
| Min SDK | API 26 (Android 8.0) | Covers ~95% of active devices as of 2026 |
| Target SDK | API 35 (Android 15) | Latest stable |

---

## 8. Out of Scope (v1)

- iOS or cross-platform (Flutter, RN, KMP) version
- Remote backend, REST API, or any cloud service beyond YouTube Data API for search
- User accounts, authentication, or cloud backup
- Social features: sharing plans, exporting to other fitness apps, leaderboards
- AI-generated workout recommendations or adaptive plan adjustment
- Nutrition or calorie tracking
- Barcode / NFC / wearable integrations
- Paid tier, in-app purchases, or ads
- Localization / internationalization (English only in v1)
- Tablet / foldable optimized layouts (phone form factor only in v1)
- Dark mode (can be added as a quick follow-up; not a blocker for v1)

---

## 9. Open Questions

1. **YouTube API quota:** The free tier allows 10,000 units/day. Search costs 100 units/query. At scale this could be a constraint — consider caching search results locally or prompting users to paste URLs as the primary path.
2. **Seed library size:** What is the minimum viable seed count? Suggested: 150–200 exercises across all muscle groups to ensure generation always has enough variety for any filter combination.
3. **Split mapping edge cases:** What happens if the user selects PPL but does not select any push muscle groups? Generation should warn and fall back gracefully rather than producing an empty day.
4. **Weight units:** kg vs lb — should this be a user preference in v1 or hard-coded to kg?
5. **Exercise ordering within a session:** Random, sorted by muscle group, or ordered by compound-before-isolation? This affects plan quality and should be a defined rule, not undefined behavior.
