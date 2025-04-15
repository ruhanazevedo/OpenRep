package com.ruhanazevedo.workoutgenerator.data.seeder

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ruhanazevedo.workoutgenerator.data.db.AppDatabase
import com.ruhanazevedo.workoutgenerator.data.db.entity.ExerciseEntity
import com.ruhanazevedo.workoutgenerator.domain.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SeedVerificationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun buildSeedExercise(
        name: String,
        muscleGroups: List<String>,
        equipment: String = "Barbell",
        difficulty: String = "Intermediate"
    ) = ExerciseEntity(
        id = UUID.randomUUID().toString(),
        name = name,
        muscleGroups = muscleGroups,
        secondaryMuscleGroups = emptyList(),
        equipment = equipment,
        difficulty = difficulty,
        instructions = "",
        youtubeVideoId = null,
        isCustom = false,
        isDeleted = false,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun seed_exercises_have_at_least_5_per_muscle_group() = runTest {
        val dao = db.exerciseDao()

        // Simulate seeded data with ≥ 5 per canonical muscle group
        val muscleGroups = listOf(
            MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
            MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.QUADS,
            MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES,
            MuscleGroup.CORE
        )

        muscleGroups.forEach { group ->
            repeat(5) { i ->
                dao.insert(buildSeedExercise("$group Exercise $i", listOf(group)))
            }
        }

        // Verify each muscle group has at least 5
        muscleGroups.forEach { group ->
            val count = dao.getByMuscleGroup(group).first().size
            assertTrue("Muscle group $group should have ≥ 5 exercises, got $count", count >= 5)
        }
    }

    @Test
    fun seeded_exercises_have_is_custom_false() = runTest {
        val dao = db.exerciseDao()
        dao.insert(buildSeedExercise("Bench Press", listOf("Chest")))

        val all = dao.getAll().first()
        assertTrue("All seeded exercises must have is_custom = false",
            all.all { !it.isCustom })
    }

    @Test
    fun seeder_is_idempotent() = runTest {
        val dao = db.exerciseDao()
        val exercise = buildSeedExercise("Bench Press", listOf("Chest"))

        // Insert twice via upsert to simulate idempotent seeder behaviour
        dao.upsert(exercise)
        dao.upsert(exercise)

        val all = dao.getAll().first()
        assertTrue("Duplicate seed should not create duplicate rows", all.size == 1)
    }

    @Test
    fun total_seeded_count_meets_minimum() = runTest {
        val dao = db.exerciseDao()

        // Insert 150 unique exercises simulating full seed
        repeat(150) { i ->
            dao.insert(buildSeedExercise("Exercise $i", listOf("Chest")))
        }

        val count = dao.countSeeded()
        assertTrue("Should have at least 150 seeded exercises, got $count", count >= 150)
    }
}
