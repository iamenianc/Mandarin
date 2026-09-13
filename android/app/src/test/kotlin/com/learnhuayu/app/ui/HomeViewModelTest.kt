package com.learnhuayu.app.ui

import androidx.compose.runtime.Composable
import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.core.ui.FeatureDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    private class FakeDestination(
        override val id: String,
        override val title: String,
    ) : FeatureDestination {
        @Composable
        override fun Content(onBack: () -> Unit) = Unit
    }

    @Test
    fun `registered feature destinations surface to home`() {
        val viewModel = HomeViewModel(
            registry = ModuleRegistry(emptySet()),
            featureDestinations = setOf(FakeDestination(id = "raymond", title = "Raymond")),
        )

        assertEquals(listOf("raymond"), viewModel.uiState.value.features.map { it.id })
        assertEquals(listOf("Raymond"), viewModel.uiState.value.features.map { it.title })
    }

    @Test
    fun `feature destinations are ordered by title`() {
        val viewModel = HomeViewModel(
            registry = ModuleRegistry(emptySet()),
            featureDestinations = setOf(
                FakeDestination(id = "zulu", title = "Zulu"),
                FakeDestination(id = "raymond", title = "Raymond"),
            ),
        )

        assertEquals(listOf("raymond", "zulu"), viewModel.uiState.value.features.map { it.id })
    }
}
