package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import com.ruhanazevedo.workoutgenerator.data.db.entity.UserPreferencesEntity
import com.ruhanazevedo.workoutgenerator.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakePrefsFlow: MutableStateFlow<UserPreferencesEntity>
    private lateinit var repository: PreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    // Captured updates
    private val updates = mutableListOf<UserPreferencesEntity>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakePrefsFlow = MutableStateFlow(UserPreferencesEntity())

        repository = PreferencesRepository(
            dao = FakeUserPreferencesDao(fakePrefsFlow, updates)
        )

        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_preferences_have_correct_defaults() = runTest {
        advanceUntilIdle()
        val prefs = viewModel.preferences.value
        assertEquals(60, prefs.restTimerSeconds)
        assertEquals(3, prefs.exercisesPerMuscleGroup)
        assertEquals("Beginner", prefs.minDifficulty)
        assertEquals("Advanced", prefs.maxDifficulty)
        assertTrue(prefs.availableEquipment.isEmpty())
    }

    @Test
    fun setRestTimerSeconds_persists_new_value() = runTest {
        advanceUntilIdle()
        viewModel.setRestTimerSeconds(90)
        advanceUntilIdle()
        assertTrue("Expected at least one update", updates.isNotEmpty())
        assertEquals(90, updates.last().restTimerSeconds)
    }

    @Test
    fun setAvailableEquipment_persists_selection() = runTest {
        advanceUntilIdle()
        viewModel.setAvailableEquipment(listOf("Barbell", "Dumbbell"))
        advanceUntilIdle()
        assertEquals(listOf("Barbell", "Dumbbell"), updates.last().availableEquipment)
    }

    @Test
    fun setMinDifficulty_persists_value() = runTest {
        advanceUntilIdle()
        viewModel.setMinDifficulty("Intermediate")
        advanceUntilIdle()
        assertEquals("Intermediate", updates.last().minDifficulty)
    }

    @Test
    fun setMaxDifficulty_persists_value() = runTest {
        advanceUntilIdle()
        viewModel.setMaxDifficulty("Intermediate")
        advanceUntilIdle()
        assertEquals("Intermediate", updates.last().maxDifficulty)
    }

    @Test
    fun setExercisesPerMuscleGroup_persists_value() = runTest {
        advanceUntilIdle()
        viewModel.setExercisesPerMuscleGroup(5)
        advanceUntilIdle()
        assertEquals(5, updates.last().exercisesPerMuscleGroup)
    }
}
