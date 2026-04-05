# Session Log — 2026-04-03

## Bug: Library → Generate transition shows Library content in background

**Investigated by:** bug agent

**Symptom:** When navigating from Library (bottom nav) to Generate (bottom nav) and then tapping "Create Workout Plan" to go to GenerateFilterScreen, the Library screen content remains visible through the slide-in animation — the incoming screen is transparent/unclipped during the transition.

**Root cause identified:** See full spec below. Two compounding issues:

1. `GenerateScreen` has no `Scaffold` — it is a bare `Column`. During the NavHost slide transition, the exiting `LibraryScreen` (which has its own `Scaffold`) remains in the compositor while the entering `GenerateScreen` slides in with no opaque background surface behind it. The `NavHost` composable itself carries no background; only the outer `Scaffold` in `WorkoutGeneratorNavHost` provides one, but that background is only as opaque as the Material3 default `Surface` color — and without an explicit `Surface` wrapping the `NavHost` content area, the slide transition renders the entering composable over the exiting one without clipping.

2. More critically for the specific reported path (Library → Generate → "Generate" button): the `GenerateScreen` does not own a `Scaffold` or `Surface`. When the slide transition `enterTransition = { slideInHorizontally(initialOffsetX = { it }) }` runs, `GenerateScreen`'s `Column` with `fillMaxSize` has a transparent background. The `LibraryScreen` exit composable (`exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) }`) is still being drawn underneath. Because `GenerateScreen` has no background color, the Library list content shows through during the ~300 ms animation.

**Evidence:**

- `WorkoutGeneratorNavHost.kt:92-99` — NavHost has slide transitions defined but no background or `Surface` applied to the content area.
- `GenerateScreen.kt:22-41` — `GenerateScreen` is a bare `Column` with no `Scaffold`, no `Surface`, no background color. Contrast with `LibraryScreen.kt:106-177` which wraps in `Scaffold`, and `GenerateFilterScreen.kt:49+` which also wraps in `Scaffold`. `GenerateScreen` is the only top-level nav destination missing an opaque container.
- The outer `Scaffold` in `WorkoutGeneratorNavHost.kt:59` provides the app chrome (bottom bar) but does not fill the `NavHost` content area with an opaque surface color — Compose does not automatically add a background to the `NavHost` padding area.

**Affected scenario:** Any transition that enters or exits `GenerateScreen` via a slide animation: Library→Generate, History→Generate, Settings→Generate (any bottom nav tap that lands on Generate).

**Fix spec for dev:**

Wrap `GenerateScreen`'s root `Column` in a `Surface` (or add `background(MaterialTheme.colorScheme.background)` modifier) so the composable is fully opaque during transitions. Alternatively, wrap `GenerateScreen` in a `Scaffold` (consistent with all other top-level screens). The simplest one-line fix is adding `.background(MaterialTheme.colorScheme.background)` to the `Column`'s `Modifier` chain, or replacing `Column(...)` with `Surface(modifier = Modifier.fillMaxSize()) { Column(...) }`.

**Status:** Handed to dev.

---

## Session screen redesign — 3 draft issues filed to OpenRep board

**Filed by:** pm agent (2026-04-04)

3 draft issues created on the OpenRep private project board (PVT_kwHOAj7m584BTPdy):

| Item ID | Title |
|---------|-------|
| PVTI_lAHOAj7m584BTPdyzgpIKRw | Session screen — day selection before starting |
| PVTI_lAHOAj7m584BTPdyzgpIKR4 | Session screen — exercise list view with expand-to-log and quick-complete |
| PVTI_lAHOAj7m584BTPdyzgpIKSA | Session screen — elapsed workout timer in top bar |

Status: Draft — awaiting Ruhan approval.

---

## Sprint 2 — Core Training Experience — 4 draft issues filed to OpenRep board

**Filed by:** pm agent (2026-04-04)

4 draft issues created on the OpenRep private project board (PVT_kwHOAj7m584BTPdy). Status set to "Todo". No Priority or Iteration fields available on the board.

Note: `updateProjectV2ItemFieldByName` and `updateProjectV2ItemField` GraphQL mutations are not available at this API endpoint. Used `gh project item-edit` CLI to set Status successfully.

| Item ID | Title | Priority | Type |
|---------|-------|----------|------|
| PVTI_lAHOAj7m584BTPdyzgpJI_I | [BUG] Quick-complete then expand logs phantom extra set | High | Bug |
| PVTI_lAHOAj7m584BTPdyzgpJJAY | Stretching & warm-up exercise category | High | Feature |
| PVTI_lAHOAj7m584BTPdyzgpJJA8 | Post-workout summary screen | Medium | Feature |
| PVTI_lAHOAj7m584BTPdyzgpJJBY | Session view: inline exercise images | Low | Enhancement |

Status: Todo — awaiting Ruhan approval.
