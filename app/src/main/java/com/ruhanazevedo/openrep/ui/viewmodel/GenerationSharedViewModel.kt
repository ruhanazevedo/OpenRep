package com.ruhanazevedo.openrep.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ruhanazevedo.openrep.domain.model.GenerationInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class GenerationSharedViewModel @Inject constructor() : ViewModel() {

    private val _input = MutableStateFlow<GenerationInput?>(null)
    val input: StateFlow<GenerationInput?> = _input

    private val _generationTrigger = MutableStateFlow(0)
    val generationTrigger: StateFlow<Int> = _generationTrigger

    fun setInput(input: GenerationInput) {
        _input.value = input
        _generationTrigger.value = _generationTrigger.value + 1
    }
}
