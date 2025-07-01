package com.ruhanazevedo.workoutgenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ruhanazevedo.workoutgenerator.domain.model.GenerationInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class GenerationSharedViewModel @Inject constructor() : ViewModel() {

    private val _input = MutableStateFlow<GenerationInput?>(null)
    val input: StateFlow<GenerationInput?> = _input

    fun setInput(input: GenerationInput) {
        _input.value = input
    }
}
