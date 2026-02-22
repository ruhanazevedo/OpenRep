---
name: YouTube WebView bugs — 2026-04-01
description: Root cause analysis for black screen flash (Bug 1) and unplayable video (Bug 2) in ExerciseDetailScreen YouTubeWebView composable
type: project
---

# Session — 2026-04-01

## Files read
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/ui/screens/ExerciseDetailScreen.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/data/db/entity/ExerciseEntity.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/domain/model/Exercise.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/data/mapper/ExerciseMapper.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/data/seeder/DatabaseSeeder.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/data/seeder/SeedExercise.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/data/repository/ExerciseRepository.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/ui/viewmodel/ExerciseDetailViewModel.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/ui/viewmodel/SearchYouTubeViewModel.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/ui/screens/SearchYouTubeScreen.kt`
- `app/src/main/java/com/ruhanazevedo/workoutgenerator/WorkoutGeneratorNavHost.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/seed_exercises.json`

## Findings

### Bug 1 (black screen flash): NOT caused by empty string vs null
Seed data omits `youtube_video_id` for exercises without videos (confirmed in seed_exercises.json — the field is simply absent). Moshi maps absent/null JSON fields to `null` in `SeedExercise` (default = null, line 14 of SeedExercise.kt). `SearchYouTubeViewModel.confirmVideoId` always writes a real 11-char ID (validated by parseYouTubeId). So `youtubeVideoId` is never "". The null guard at ExerciseDetailScreen.kt:175 is structurally correct.

Root cause is the `AndroidView` factory/update split and Compose recomposition. See full spec below.

### Bug 2 (unplayable video): Missing WebChromeClient + loadDataWithBaseURL limitations
No WebChromeClient is set. See full spec below.

**Why:** android.permission.hardwareAccelerated is absent from the activity tag in AndroidManifest.xml (only the app-level theme is set, no explicit hardwareAccelerated on <activity>).
