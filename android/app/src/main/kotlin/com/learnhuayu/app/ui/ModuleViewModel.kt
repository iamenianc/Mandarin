package com.learnhuayu.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.core.model.LessonSpec
import com.learnhuayu.core.model.PracticeSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModuleUiState(
    val moduleId: String = "",
    val title: String? = null,
    val loading: Boolean = false,
    val notFound: Boolean = false,
    val lessons: List<LessonSpec> = emptyList(),
    val practices: List<PracticeSpec> = emptyList(),
)

/**
 * Loads one module's lesson and practice specs. The registry needs only id/title
 * synchronously; the bundled corpus resolves the specs asynchronously
 * (`LearningModule.lessons()`/`practices()` are suspend).
 */
@HiltViewModel
class ModuleViewModel @Inject constructor(
    private val registry: ModuleRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModuleUiState())
    val uiState: StateFlow<ModuleUiState> = _uiState.asStateFlow()

    fun load(moduleId: String) {
        if (_uiState.value.moduleId == moduleId && !_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.value = ModuleUiState(moduleId = moduleId, loading = true)
            try {
                val module = registry.module(moduleId)
                if (module == null) {
                    _uiState.value = ModuleUiState(moduleId = moduleId, notFound = true)
                    return@launch
                }
                val lessons = module.lessons()
                val practices = module.practices()
                _uiState.value = ModuleUiState(
                    moduleId = moduleId,
                    title = module.title,
                    lessons = lessons,
                    practices = practices,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(loading = false, notFound = true) }
            }
        }
    }
}
