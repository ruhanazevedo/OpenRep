package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruhanazevedo.workoutgenerator.data.repository.ExerciseRepository
import com.ruhanazevedo.workoutgenerator.domain.model.Difficulty
import com.ruhanazevedo.workoutgenerator.domain.model.Equipment
import com.ruhanazevedo.workoutgenerator.domain.model.Exercise
import com.ruhanazevedo.workoutgenerator.domain.model.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: List<String>
)

data class DuplicateEntry(val name: String, val incoming: Exercise, val existingId: String)

sealed class ImportDuplicatePrompt {
    data class Pending(val entry: DuplicateEntry) : ImportDuplicatePrompt()
    data object None : ImportDuplicatePrompt()
}

data class LibraryUiState(
    val exercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val selectedMuscleGroups: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val importResult: ImportResult? = null,
    val duplicatePrompt: ImportDuplicatePrompt = ImportDuplicatePrompt.None
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedMuscleGroups = MutableStateFlow<Set<String>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val exercises = combine(searchQuery, selectedMuscleGroups) { query, muscles ->
        query to muscles
    }.flatMapLatest { (query, muscles) ->
        val muscleFilter = if (muscles.isEmpty()) "" else muscles.first()
        repository.search(query.trim(), muscleFilter)
    }

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    // State for in-progress import batch
    private var pendingDuplicates = ArrayDeque<DuplicateEntry>()
    private var importedCount = 0
    private var skippedCount = 0
    private val importErrors = mutableListOf<String>()

    init {
        viewModelScope.launch {
            combine(exercises, searchQuery, selectedMuscleGroups) { list, query, muscles ->
                Triple(list, query, muscles)
            }.collect { (list, query, muscles) ->
                val filtered = if (muscles.size > 1) {
                    list.filter { exercise ->
                        muscles.any { muscle ->
                            exercise.muscleGroups.any { it.equals(muscle, ignoreCase = true) }
                        }
                    }
                } else {
                    list
                }
                _uiState.value = _uiState.value.copy(
                    exercises = filtered,
                    searchQuery = query,
                    selectedMuscleGroups = muscles,
                    isLoading = false
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleMuscleGroup(muscle: String) {
        val current = selectedMuscleGroups.value.toMutableSet()
        if (muscle in current) current.remove(muscle) else current.add(muscle)
        selectedMuscleGroups.value = current
    }

    fun clearMuscleFilters() {
        selectedMuscleGroups.value = emptySet()
    }

    fun dismissImportResult() {
        _uiState.value = _uiState.value.copy(importResult = null)
    }

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val json = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            }.getOrNull() ?: return@launch

            processImportJson(json)
        }
    }

    private suspend fun processImportJson(json: String) {
        val array = runCatching { JSONArray(json) }.getOrElse {
            _uiState.value = _uiState.value.copy(
                importResult = ImportResult(0, 0, listOf("Invalid JSON: ${it.message}"))
            )
            return
        }

        importedCount = 0
        skippedCount = 0
        importErrors.clear()
        pendingDuplicates.clear()

        val toInsert = mutableListOf<Exercise>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: run {
                importErrors.add("Entry $i: not an object")
                skippedCount++
                continue
            }
            val (exercise, error) = parseEntry(obj, i)
            if (error != null) {
                importErrors.add(error)
                skippedCount++
            } else if (exercise != null) {
                toInsert.add(exercise)
            }
        }

        for (exercise in toInsert) {
            val existing = repository.findByNameIgnoreCase(exercise.name)
            if (existing != null) {
                pendingDuplicates.addLast(DuplicateEntry(exercise.name, exercise, existing.id))
            } else {
                repository.insert(exercise)
                importedCount++
            }
        }

        if (pendingDuplicates.isNotEmpty()) {
            showNextDuplicate()
        } else {
            finishImport()
        }
    }

    private fun showNextDuplicate() {
        val next = pendingDuplicates.firstOrNull()
        if (next == null) {
            finishImport()
            return
        }
        _uiState.value = _uiState.value.copy(duplicatePrompt = ImportDuplicatePrompt.Pending(next))
    }

    fun resolveImportDuplicate(overwrite: Boolean) {
        val prompt = _uiState.value.duplicatePrompt
        if (prompt !is ImportDuplicatePrompt.Pending) return
        val entry = prompt.entry
        pendingDuplicates.removeFirst()
        viewModelScope.launch {
            if (overwrite) {
                repository.upsert(entry.incoming.copy(id = entry.existingId))
                importedCount++
            } else {
                skippedCount++
            }
            _uiState.value = _uiState.value.copy(duplicatePrompt = ImportDuplicatePrompt.None)
            if (pendingDuplicates.isNotEmpty()) {
                showNextDuplicate()
            } else {
                finishImport()
            }
        }
    }

    private fun finishImport() {
        _uiState.value = _uiState.value.copy(
            importResult = ImportResult(importedCount, skippedCount, importErrors.toList()),
            duplicatePrompt = ImportDuplicatePrompt.None
        )
    }

    private fun parseEntry(obj: JSONObject, index: Int): Pair<Exercise?, String?> {
        val name = obj.optString("name").trim()
        if (name.isBlank()) return null to "Entry $index: missing name"

        val rawMuscles = obj.optJSONArray("muscle_groups")
        if (rawMuscles == null || rawMuscles.length() == 0) {
            return null to "Entry $index ($name): missing muscle_groups"
        }
        val muscles = (0 until rawMuscles.length()).map { rawMuscles.getString(it) }
        val invalidMuscles = muscles.filter { it !in MuscleGroup.ALL }
        if (invalidMuscles.isNotEmpty()) {
            return null to "Entry $index ($name): invalid muscle_groups $invalidMuscles"
        }

        val rawSecondary = obj.optJSONArray("secondary_muscle_groups")
        val secondary = if (rawSecondary != null) {
            (0 until rawSecondary.length()).map { rawSecondary.getString(it) }
        } else emptyList()

        val equipment = obj.optString("equipment").trim()
        if (equipment !in Equipment.ALL) {
            return null to "Entry $index ($name): invalid equipment '$equipment'"
        }

        val difficultyStr = obj.optString("difficulty").trim()
        if (difficultyStr.isBlank() || Difficulty.entries.none { it.name.equals(difficultyStr, ignoreCase = true) }) {
            return null to "Entry $index ($name): invalid difficulty '$difficultyStr'"
        }

        val instructions = obj.optString("instructions")
        val rawVideoId = obj.optString("youtube_video_id").trim().ifBlank { null }
        val videoId = rawVideoId?.let { extractYouTubeId(it) }

        return Exercise(
            id = UUID.randomUUID().toString(),
            name = name,
            muscleGroups = muscles,
            secondaryMuscleGroups = secondary,
            equipment = equipment,
            difficulty = Difficulty.from(difficultyStr),
            instructions = instructions,
            youtubeVideoId = videoId,
            isCustom = true,
            isDeleted = false,
            createdAt = System.currentTimeMillis()
        ) to null
    }
}

fun extractYouTubeId(input: String): String {
    if (input.length == 11 && !input.contains("/") && !input.contains("?")) return input
    val pattern = Regex("(?:v=|youtu\\.be/|embed/)([A-Za-z0-9_-]{11})")
    return pattern.find(input)?.groupValues?.get(1) ?: input.takeLast(11)
}
