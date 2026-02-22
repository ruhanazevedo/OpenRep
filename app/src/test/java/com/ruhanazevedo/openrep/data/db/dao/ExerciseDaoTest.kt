package com.ruhanazevedo.openrep.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ruhanazevedo.openrep.data.db.AppDatabase
import com.ruhanazevedo.openrep.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExerciseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun buildExercise(
        id: String = "ex-1",
        name: String = "Bench Press",
        muscleGroups: List<String> = listOf("Chest"),
        equipment: String = "Barbell",
        difficulty: String = "Intermediate",
        isCustom: Boolean = false,
        isDeleted: Boolean = false
    ) = ExerciseEntity(
        id = id,
        name = name,
        muscleGroups = muscleGroups,
        secondaryMuscleGroups = emptyList(),
        equipment = equipment,
        difficulty = difficulty,
        instructions = "",
        youtubeVideoId = null,
        isCustom = isCustom,
        isDeleted = isDeleted,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun insert_and_getAll() = runTest {
        dao.insert(buildExercise(id = "ex-1", name = "Bench Press"))
        dao.insert(buildExercise(id = "ex-2", name = "Squat"))

        val all = dao.getAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun getById_returns_correct_exercise() = runTest {
        dao.insert(buildExercise(id = "ex-1", name = "Bench Press"))
        dao.insert(buildExercise(id = "ex-2", name = "Squat"))

        val result = dao.getById("ex-1").first()
        assertNotNull(result)
        assertEquals("Bench Press", result!!.name)
    }

    @Test
    fun getById_returns_null_when_not_found() = runTest {
        val result = dao.getById("nonexistent").first()
        assertNull(result)
    }

    @Test
    fun softDelete_sets_is_deleted_flag() = runTest {
        dao.insert(buildExercise(id = "ex-1"))
        dao.softDelete("ex-1")

        val all = dao.getAll().first()
        assertTrue("Soft deleted exercise should not appear in getAll", all.isEmpty())
    }

    @Test
    fun getByMuscleGroup_filters_correctly() = runTest {
        dao.insert(buildExercise(id = "ex-1", name = "Bench Press", muscleGroups = listOf("Chest")))
        dao.insert(buildExercise(id = "ex-2", name = "Squat", muscleGroups = listOf("Quads", "Glutes")))
        dao.insert(buildExercise(id = "ex-3", name = "Cable Fly", muscleGroups = listOf("Chest")))

        val chest = dao.getByMuscleGroup("Chest").first()
        assertEquals(2, chest.size)
        assertTrue(chest.all { it.muscleGroups.contains("Chest") })
    }

    @Test
    fun getByMuscleGroup_includes_multi_group_exercises() = runTest {
        dao.insert(buildExercise(id = "ex-1", muscleGroups = listOf("Chest", "Triceps")))

        val chest = dao.getByMuscleGroup("Chest").first()
        val triceps = dao.getByMuscleGroup("Triceps").first()

        assertEquals(1, chest.size)
        assertEquals(1, triceps.size)
    }

    @Test
    fun update_modifies_existing_exercise() = runTest {
        val original = buildExercise(id = "ex-1", name = "Bench Press")
        dao.insert(original)

        val updated = original.copy(name = "Incline Bench Press")
        dao.update(updated)

        val result = dao.getById("ex-1").first()
        assertEquals("Incline Bench Press", result!!.name)
    }

    @Test
    fun upsert_replaces_on_conflict() = runTest {
        val original = buildExercise(id = "ex-1", name = "Bench Press")
        dao.upsert(original)

        val replacement = original.copy(name = "Incline Bench Press")
        dao.upsert(replacement)

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Incline Bench Press", all[0].name)
    }

    @Test
    fun countSeeded_returns_non_custom_count() = runTest {
        dao.insert(buildExercise(id = "ex-1", isCustom = false))
        dao.insert(buildExercise(id = "ex-2", isCustom = false))
        dao.insert(buildExercise(id = "ex-3", isCustom = true))

        assertEquals(2, dao.countSeeded())
    }

    @Test
    fun getAll_excludes_soft_deleted() = runTest {
        dao.insert(buildExercise(id = "ex-1"))
        dao.insert(buildExercise(id = "ex-2"))
        dao.softDelete("ex-1")

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("ex-2", all[0].id)
    }
}
