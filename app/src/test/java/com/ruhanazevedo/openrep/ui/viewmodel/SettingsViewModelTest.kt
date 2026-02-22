package com.ruhanazevedo.openrep.ui.viewmodel

import com.ruhanazevedo.openrep.data.db.entity.UserPreferencesEntity
import com.ruhanazevedo.openrep.data.repository.PreferencesRepository
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

    @Test
    fun setMinDifficulty_above_max_is_rejected() = runTest {
        // Default: min=Beginner, max=Advanced. Trying to set min=Advanced is allowed (equal is ok).
        // Setting min beyond max: set max to Intermediate first, then try min=Advanced (above Intermediate).
        advanceUntilIdle()
        viewModel.setMaxDifficulty("Intermediate")
        advanceUntilIdle()
        val countBefore = updates.size
        viewModel.setMinDifficulty("Advanced") // Advanced > Intermediate — should be rejected
        advanceUntilIdle()
        // An update is still issued (with unchanged value), so check min was NOT changed
        assertEquals("Beginner", updates.last().minDifficulty)
    }

    @Test
    fun setMaxDifficulty_below_min_is_rejected() = runTest {
        // Default: min=Beginner, max=Advanced. Set min=Intermediate first, then try max=Beginner (below Intermediate).
        advanceUntilIdle()
        viewModel.setMinDifficulty("Intermediate")
        advanceUntilIdle()
        viewModel.setMaxDifficulty("Beginner") // Beginner < Intermediate — should be rejected
        advanceUntilIdle()
        assertEquals("Advanced", updates.last().maxDifficulty)
    }

    @Test
    fun setMinDifficulty_valid_within_range_is_accepted() = runTest {
        // Default: min=Beginner, max=Advanced. Setting min=Intermediate is valid.
        advanceUntilIdle()
        viewModel.setMinDifficulty("Intermediate")
        advanceUntilIdle()
        assertEquals("Intermediate", updates.last().minDifficulty)
        // Also valid: setting max=Intermediate when min=Intermediate (equal boundary)
        viewModel.setMaxDifficulty("Intermediate")
        advanceUntilIdle()
        assertEquals("Intermediate", updates.last().maxDifficulty)
    }
}
