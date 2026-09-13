package com.learnhuayu.app.ui

import androidx.lifecycle.ViewModel
import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.core.model.LearningModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class HomeUiState(
    val modules: List<LearningModule> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val registry: ModuleRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(modules = registry.modules))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun module(id: String): LearningModule? = registry.module(id)
}
