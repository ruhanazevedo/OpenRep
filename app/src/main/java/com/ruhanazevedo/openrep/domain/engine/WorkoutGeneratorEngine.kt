package com.ruhanazevedo.openrep.domain.engine

import com.ruhanazevedo.openrep.data.db.dao.ExerciseDao
import com.ruhanazevedo.openrep.data.db.entity.ExerciseEntity
import com.ruhanazevedo.openrep.data.repository.PreferencesRepository
import com.ruhanazevedo.openrep.domain.model.Difficulty
import com.ruhanazevedo.openrep.domain.model.GeneratedDay
import com.ruhanazevedo.openrep.domain.model.GeneratedExercise
import com.ruhanazevedo.openrep.domain.model.GeneratedPlan
import com.ruhanazevedo.openrep.domain.model.GenerationInput
import com.ruhanazevedo.openrep.domain.model.WarmupCooldownItem
import com.ruhanazevedo.openrep.domain.model.MuscleGroup
import com.ruhanazevedo.openrep.domain.model.SplitType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutGeneratorEngine @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val preferencesRepository: PreferencesRepository
) {

    private val pushMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, MuscleGroup.FULL_BODY)
    private val pullMuscles = setOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FULL_BODY)
    private val legMuscles = setOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.FULL_BODY)

    suspend fun generate(input: GenerationInput): GeneratedPlan {
        val prefs = preferencesRepository.preferences.first()

        val equipmentFilter = if (prefs.availableEquipment.isEmpty()) "" else "filtered"
        val equipmentList = prefs.availableEquipment.ifEmpty { emptyList() }

        val minDiff = Difficulty.from(prefs.minDifficulty)
        val maxDiff = Difficulty.from(prefs.maxDifficulty)
        val difficultyList = Difficulty.entries
            .filter { it >= minDiff && it <= maxDiff }
            .map { it.name }

        val setsReps = setsRepsScheme(maxDiff)

        // Time-aware trimming: reserve time for warmup/cooldown then cap exercises per day
        val warmupCooldownBudgetSeconds = if (input.includeWarmupCooldown) 10 * 60 else 0
        val availableMainSeconds = (input.sessionDurationMinutes * 60) - warmupCooldownBudgetSeconds
        val secondsPerExercise = setsReps.first * 150 // ~2.5 min per set including rest
        val maxExercisesPerDay = (availableMainSeconds / secondsPerExercise).coerceAtLeast(1)

        val sessionTemplates = buildSessionTemplates(input, equipmentFilter, equipmentList, difficultyList, setsReps)
            .map { it.take(maxExercisesPerDay) }

        if (sessionTemplates.isEmpty()) {
            return GeneratedPlan(
                splitType = input.splitType,
                daysPerWeek = input.daysPerWeek,
                muscleGroups = input.muscleGroups,
                days = emptyList()
            )
        }

        val days = (0 until input.daysPerWeek).map { dayIdx ->
            val template = sessionTemplates[dayIdx % sessionTemplates.size]
            val muscles = template.map { it.targetMuscle }.distinct()
            val warmup = if (input.includeWarmupCooldown) buildWarmup(muscles) else emptyList()
            val cooldown = if (input.includeWarmupCooldown) buildCooldown(muscles) else emptyList()
            val estimatedMinutes = estimateMinutes(template, warmup, cooldown)
            GeneratedDay(
                dayIndex = dayIdx,
                label = "Day ${dayIdx + 1}",
                exercises = template,
                warmup = warmup,
                cooldown = cooldown,
                estimatedMinutes = estimatedMinutes
            )
        }

        return GeneratedPlan(
            splitType = input.splitType,
            daysPerWeek = input.daysPerWeek,
            muscleGroups = input.muscleGroups,
            days = days
        )
    }

    private suspend fun buildSessionTemplates(
        input: GenerationInput,
        equipmentFilter: String,
        equipmentList: List<String>,
        difficultyList: List<String>,
        setsReps: Pair<Int, Int>
    ): List<List<GeneratedExercise>> {
        return when (input.splitType) {
            SplitType.A -> {
                val session = buildSessionForMuscles(
                    input.muscleGroups, input.exercisesPerMuscle,
                    equipmentFilter, equipmentList, difficultyList, setsReps
                )
                if (session.isEmpty()) emptyList() else listOf(session)
            }
            SplitType.AA -> buildAntagonistSessions(
                input.muscleGroups, input.exercisesPerMuscle,
                equipmentFilter, equipmentList, difficultyList, setsReps
            )
            SplitType.AB -> {
                val halves = splitMusclesInto(input.muscleGroups, 2)
                halves.mapNotNull { muscles ->
                    val session = buildSessionForMuscles(
                        muscles, input.exercisesPerMuscle,
                        equipmentFilter, equipmentList, difficultyList, setsReps
                    )
                    session.ifEmpty { null }
                }
            }
            SplitType.ABC -> {
                val thirds = splitMusclesInto(input.muscleGroups, 3)
                thirds.mapNotNull { muscles ->
                    val session = buildSessionForMuscles(
                        muscles, input.exercisesPerMuscle,
                        equipmentFilter, equipmentList, difficultyList, setsReps
                    )
                    session.ifEmpty { null }
                }
            }
            SplitType.PPL -> buildPplSessions(
                input.muscleGroups, input.exercisesPerMuscle,
                equipmentFilter, equipmentList, difficultyList, setsReps
            )
        }
    }

    private suspend fun buildPplSessions(
        selectedMuscles: List<String>,
        exercisesPerMuscle: Int,
        equipmentFilter: String,
        equipmentList: List<String>,
        difficultyList: List<String>,
        setsReps: Pair<Int, Int>
    ): List<List<GeneratedExercise>> {
        val selected = selectedMuscles.toSet()
        val coreSelected = selected.contains(MuscleGroup.CORE)

        val pushSelected = pushMuscles.filter { it in selected }
        val pullSelected = pullMuscles.filter { it in selected }
        val legsSelected = legMuscles.filter { it in selected }

        val sessions = mutableListOf<List<GeneratedExercise>>()

        if (pushSelected.isNotEmpty()) {
            val muscles = if (coreSelected) pushSelected + MuscleGroup.CORE else pushSelected
            val session = buildSessionForMuscles(
                muscles, exercisesPerMuscle, equipmentFilter, equipmentList, difficultyList, setsReps
            )
            if (session.isNotEmpty()) sessions.add(session)
        }
        if (pullSelected.isNotEmpty()) {
            val muscles = if (coreSelected) pullSelected + MuscleGroup.CORE else pullSelected
            val session = buildSessionForMuscles(
                muscles, exercisesPerMuscle, equipmentFilter, equipmentList, difficultyList, setsReps
            )
            if (session.isNotEmpty()) sessions.add(session)
        }
        if (legsSelected.isNotEmpty()) {
            val muscles = if (coreSelected) legsSelected + MuscleGroup.CORE else legsSelected
            val session = buildSessionForMuscles(
                muscles, exercisesPerMuscle, equipmentFilter, equipmentList, difficultyList, setsReps
            )
            if (session.isNotEmpty()) sessions.add(session)
        }
        // If only Core selected (no push/pull/legs), make a single session
        if (sessions.isEmpty() && coreSelected) {
            val session = buildSessionForMuscles(
                listOf(MuscleGroup.CORE), exercisesPerMuscle,
                equipmentFilter, equipmentList, difficultyList, setsReps
            )
            if (session.isNotEmpty()) sessions.add(session)
        }

        return sessions
    }

    private suspend fun buildAntagonistSessions(
        selectedMuscles: List<String>,
        exercisesPerMuscle: Int,
        equipmentFilter: String,
        equipmentList: List<String>,
        difficultyList: List<String>,
        setsReps: Pair<Int, Int>
    ): List<List<GeneratedExercise>> {
        val antagonisticPairs = mapOf(
            MuscleGroup.CHEST to MuscleGroup.BACK,
            MuscleGroup.BACK to MuscleGroup.CHEST,
            MuscleGroup.BICEPS to MuscleGroup.TRICEPS,
            MuscleGroup.TRICEPS to MuscleGroup.BICEPS,
            MuscleGroup.QUADS to MuscleGroup.HAMSTRINGS,
            MuscleGroup.HAMSTRINGS to MuscleGroup.QUADS,
            MuscleGroup.SHOULDERS to MuscleGroup.CORE,
            MuscleGroup.CORE to MuscleGroup.SHOULDERS,
            MuscleGroup.GLUTES to MuscleGroup.CALVES,
            MuscleGroup.CALVES to MuscleGroup.GLUTES
        )

        val selected = selectedMuscles.toSet()
        val fullBodySelected = selected.contains(MuscleGroup.FULL_BODY)
        val workMuscles = selectedMuscles.filter { it != MuscleGroup.FULL_BODY }

        if (workMuscles.isEmpty()) {
            val session = buildSessionForMuscles(
                listOf(MuscleGroup.FULL_BODY), exercisesPerMuscle,
                equipmentFilter, equipmentList, difficultyList, setsReps
            )
            return if (session.isEmpty()) emptyList() else listOf(session)
        }

        val sessionGroups = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()

        for (muscle in workMuscles) {
            if (muscle in visited) continue
            visited.add(muscle)
            val antagonist = antagonisticPairs[muscle]
            if (antagonist != null && antagonist in selected && antagonist !in visited) {
                visited.add(antagonist)
                sessionGroups.add(listOf(muscle, antagonist))
            } else {
                sessionGroups.add(listOf(muscle))
            }
        }

        if (sessionGroups.size == 1) {
            val muscles = if (fullBodySelected) sessionGroups[0] + MuscleGroup.FULL_BODY else sessionGroups[0]
            val session = buildSessionForMuscles(
                muscles, exercisesPerMuscle, equipmentFilter, equipmentList, difficultyList, setsReps
            )
            return if (session.isEmpty()) emptyList() else listOf(session)
        }

        return sessionGroups.mapNotNull { group ->
            val muscles = if (fullBodySelected) group + MuscleGroup.FULL_BODY else group
            val session = buildSessionForMuscles(
                muscles, exercisesPerMuscle, equipmentFilter, equipmentList, difficultyList, setsReps
            )
            session.ifEmpty { null }
        }
    }

    private suspend fun buildSessionForMuscles(
        muscles: List<String>,
        exercisesPerMuscle: Int,
        equipmentFilter: String,
        equipmentList: List<String>,
        difficultyList: List<String>,
        setsReps: Pair<Int, Int>
    ): List<GeneratedExercise> {
        val result = mutableListOf<GeneratedExercise>()
        muscles.forEach { muscle ->
            val candidates = exerciseDao.getByMuscleGroupFiltered(
                group = muscle,
                equipmentFilter = equipmentFilter,
                equipmentList = equipmentList,
                difficultyList = difficultyList
            )
            if (candidates.isEmpty()) return@forEach
            val picked = candidates.shuffled().take(exercisesPerMuscle)
            picked.forEach { entity ->
                result.add(entity.toGeneratedExercise(muscle, setsReps))
            }
        }
        return result
    }

    private fun splitMusclesInto(muscles: List<String>, parts: Int): List<List<String>> {
        if (muscles.isEmpty()) return emptyList()
        val buckets = Array(parts) { mutableListOf<String>() }
        muscles.forEachIndexed { idx, muscle -> buckets[idx % parts].add(muscle) }
        return buckets.filter { it.isNotEmpty() }
    }

    private fun setsRepsScheme(maxDifficulty: Difficulty): Pair<Int, Int> = when (maxDifficulty) {
        Difficulty.Beginner -> 3 to 12
        Difficulty.Intermediate -> 4 to 10
        Difficulty.Advanced -> 5 to 5
    }

    private fun ExerciseEntity.toGeneratedExercise(targetMuscle: String, setsReps: Pair<Int, Int>) =
        GeneratedExercise(
            exerciseId = id,
            name = name,
            targetMuscle = targetMuscle,
            equipment = equipment,
            sets = setsReps.first,
            reps = setsReps.second,
            youtubeVideoId = youtubeVideoId
        )

    // ── Warmup & Cooldown ────────────────────────────────────────────────────

    private fun buildWarmup(muscles: List<String>): List<WarmupCooldownItem> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<WarmupCooldownItem>()
        if (MuscleGroup.FULL_BODY in muscles) {
            warmupByMuscle[MuscleGroup.FULL_BODY]?.take(2)?.forEach { if (seen.add(it.name)) result.add(it) }
        }
        for (muscle in muscles.filter { it != MuscleGroup.FULL_BODY }) {
            warmupByMuscle[muscle]?.take(2)?.forEach { if (seen.add(it.name)) result.add(it) }
            if (result.size >= 5) break
        }
        if (result.isEmpty()) warmupByMuscle[MuscleGroup.FULL_BODY]?.forEach { result.add(it) }
        return result.take(5)
    }

    private fun buildCooldown(muscles: List<String>): List<WarmupCooldownItem> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<WarmupCooldownItem>()
        for (muscle in muscles) {
            cooldownByMuscle[muscle]?.take(2)?.forEach { if (seen.add(it.name)) result.add(it) }
            if (result.size >= 4) break
        }
        val breathing = WarmupCooldownItem("Deep Breathing", 60, "Inhale 4s, hold 4s, exhale 6s — 5 cycles")
        if (seen.add(breathing.name)) result.add(breathing)
        return result.take(5)
    }

    private fun estimateMinutes(
        exercises: List<GeneratedExercise>,
        warmup: List<WarmupCooldownItem>,
        cooldown: List<WarmupCooldownItem>
    ): Int {
        val avgSets = exercises.firstOrNull()?.sets ?: 4
        val mainSeconds = exercises.size * avgSets * 150
        val warmupSeconds = warmup.sumOf { it.durationSeconds }
        val cooldownSeconds = cooldown.sumOf { it.durationSeconds }
        return ((mainSeconds + warmupSeconds + cooldownSeconds) / 60).coerceAtLeast(1)
    }

    private val warmupByMuscle = mapOf(
        MuscleGroup.CHEST to listOf(
            WarmupCooldownItem("Arm Circles", 45, "10 large circles forward and backward"),
            WarmupCooldownItem("Band Pull-Aparts", 45, "15 reps to open the chest"),
            WarmupCooldownItem("Light Push-Ups", 30, "10 slow reps to activate the chest")
        ),
        MuscleGroup.BACK to listOf(
            WarmupCooldownItem("Cat-Cow Stretch", 45, "10 reps, focus on thoracic extension"),
            WarmupCooldownItem("Arm Swings", 30, "Cross-body swings ×15 each direction"),
            WarmupCooldownItem("Band Rows", 45, "15 light reps to activate lats")
        ),
        MuscleGroup.SHOULDERS to listOf(
            WarmupCooldownItem("Shoulder Circles", 30, "10 circles forward, 10 backward"),
            WarmupCooldownItem("Band Pull-Aparts", 45, "15 reps at shoulder height"),
            WarmupCooldownItem("Cross-Arm Dynamic Stretch", 30, "15 swings across the body")
        ),
        MuscleGroup.BICEPS to listOf(
            WarmupCooldownItem("Wrist Circles", 30, "10 circles each direction"),
            WarmupCooldownItem("Arm Swings", 30, "Elbow bends to loosen the joint"),
            WarmupCooldownItem("Light Band Curls", 30, "15 reps with minimal resistance")
        ),
        MuscleGroup.TRICEPS to listOf(
            WarmupCooldownItem("Overhead Dynamic Stretch", 30, "5 dynamic reaches each arm"),
            WarmupCooldownItem("Wrist Circles", 30, "10 circles to warm the elbow"),
            WarmupCooldownItem("Light Pushdowns", 30, "15 reps with band or cable")
        ),
        MuscleGroup.QUADS to listOf(
            WarmupCooldownItem("Leg Swings (Forward)", 45, "15 swings each leg"),
            WarmupCooldownItem("Walking Lunges", 45, "10 reps each side, bodyweight"),
            WarmupCooldownItem("Bodyweight Squats", 45, "15 slow reps with full depth")
        ),
        MuscleGroup.HAMSTRINGS to listOf(
            WarmupCooldownItem("Leg Swings (Side)", 45, "15 swings each leg"),
            WarmupCooldownItem("Hip Hinges", 45, "10 slow RDL-style hinges"),
            WarmupCooldownItem("Inchworm", 45, "5 reps, walk hands out to plank")
        ),
        MuscleGroup.GLUTES to listOf(
            WarmupCooldownItem("Hip Circles", 30, "10 circles each direction"),
            WarmupCooldownItem("Glute Bridges", 45, "15 bodyweight reps"),
            WarmupCooldownItem("Clamshells", 30, "15 reps each side")
        ),
        MuscleGroup.CALVES to listOf(
            WarmupCooldownItem("Ankle Circles", 30, "10 circles each direction"),
            WarmupCooldownItem("Calf Raises", 45, "20 bodyweight reps"),
            WarmupCooldownItem("Jump Rope (Light)", 60, "60 seconds easy pace")
        ),
        MuscleGroup.CORE to listOf(
            WarmupCooldownItem("Dead Bugs", 45, "5 reps each side, slow and controlled"),
            WarmupCooldownItem("Bird Dogs", 45, "5 reps each side"),
            WarmupCooldownItem("Hip Flexor March", 45, "Dynamic lunge walks ×10 each")
        ),
        MuscleGroup.FULL_BODY to listOf(
            WarmupCooldownItem("Jumping Jacks", 60, "50 reps to elevate heart rate"),
            WarmupCooldownItem("High Knees", 45, "30 seconds each"),
            WarmupCooldownItem("Dynamic Full-Body Stretch", 60, "Arm swings, leg swings, torso rotations")
        )
    )

    private val cooldownByMuscle = mapOf(
        MuscleGroup.CHEST to listOf(
            WarmupCooldownItem("Doorway Chest Stretch", 30, "Hold 30s, feel the stretch across pecs"),
            WarmupCooldownItem("Cross-Arm Stretch", 30, "Hold 30s each side")
        ),
        MuscleGroup.BACK to listOf(
            WarmupCooldownItem("Child's Pose", 45, "Hold 45s, arms extended overhead"),
            WarmupCooldownItem("Seated Spinal Twist", 30, "Hold 30s each side")
        ),
        MuscleGroup.SHOULDERS to listOf(
            WarmupCooldownItem("Cross-Arm Shoulder Stretch", 30, "Hold 30s each side"),
            WarmupCooldownItem("Overhead Tricep Stretch", 30, "Hold 30s each arm")
        ),
        MuscleGroup.BICEPS to listOf(
            WarmupCooldownItem("Wrist Flexor Stretch", 30, "Arm out palm up, gently press fingers down"),
            WarmupCooldownItem("Bicep Wall Stretch", 30, "Place palm on wall, rotate body away")
        ),
        MuscleGroup.TRICEPS to listOf(
            WarmupCooldownItem("Overhead Tricep Stretch", 30, "Hold 30s each arm"),
            WarmupCooldownItem("Cross-Body Arm Stretch", 30, "Hold 30s each side")
        ),
        MuscleGroup.QUADS to listOf(
            WarmupCooldownItem("Standing Quad Stretch", 30, "Hold 30s each leg"),
            WarmupCooldownItem("Kneeling Hip Flexor Stretch", 45, "Hold 45s each side")
        ),
        MuscleGroup.HAMSTRINGS to listOf(
            WarmupCooldownItem("Seated Hamstring Stretch", 45, "Hold 45s each leg"),
            WarmupCooldownItem("Standing Forward Fold", 45, "Hold 45s, slight knee bend ok")
        ),
        MuscleGroup.GLUTES to listOf(
            WarmupCooldownItem("Figure-4 Stretch", 45, "Hold 45s each side"),
            WarmupCooldownItem("Pigeon Pose", 45, "Hold 45s each side")
        ),
        MuscleGroup.CALVES to listOf(
            WarmupCooldownItem("Standing Calf Stretch", 30, "Hold 30s each leg against wall"),
            WarmupCooldownItem("Downward Dog", 45, "Hold 45s, pedal heels alternately")
        ),
        MuscleGroup.CORE to listOf(
            WarmupCooldownItem("Cobra Stretch", 30, "Hold 30s, decompress the spine"),
            WarmupCooldownItem("Knee-to-Chest Stretch", 30, "Hold 30s each side")
        ),
        MuscleGroup.FULL_BODY to listOf(
            WarmupCooldownItem("Full-Body Static Stretch", 120, "2 min of progressive head-to-toe stretching"),
            WarmupCooldownItem("Deep Breathing", 60, "Inhale 4s, hold 4s, exhale 6s — 5 cycles")
        )
    )
}

