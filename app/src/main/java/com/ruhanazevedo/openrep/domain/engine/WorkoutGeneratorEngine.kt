package com.ruhanazevedo.openrep.domain.engine

import com.ruhanazevedo.openrep.data.db.dao.ExerciseDao
import com.ruhanazevedo.openrep.data.db.entity.ExerciseEntity
import com.ruhanazevedo.openrep.data.repository.PreferencesRepository
import com.ruhanazevedo.openrep.domain.model.Difficulty
import com.ruhanazevedo.openrep.domain.model.GeneratedDay
import com.ruhanazevedo.openrep.domain.model.GeneratedExercise
import com.ruhanazevedo.openrep.domain.model.GeneratedPlan
import com.ruhanazevedo.openrep.domain.model.GenerationInput
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

        val sessionTemplates = buildSessionTemplates(input, equipmentFilter, equipmentList, difficultyList, setsReps)

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
            GeneratedDay(
                dayIndex = dayIdx,
                label = "Day ${dayIdx + 1}",
                exercises = template
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
            SplitType.AA -> {
                val session = buildSessionForMuscles(
                    input.muscleGroups, input.exercisesPerMuscle,
                    equipmentFilter, equipmentList, difficultyList, setsReps
                )
                if (session.isEmpty()) emptyList() else listOf(session)
            }
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
}
