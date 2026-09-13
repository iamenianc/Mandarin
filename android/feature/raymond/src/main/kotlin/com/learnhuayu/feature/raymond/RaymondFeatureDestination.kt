package com.learnhuayu.feature.raymond

import androidx.compose.runtime.Composable
import com.learnhuayu.core.ui.FeatureDestination
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Raymond: the ask-anything Mandarin Q&A chat (FR-25, WF-7, `docs/03-design.md`). Raymond
 * is a feature destination rather than a learning module: it ships no lessons or practice
 * and instead provides a persistent chat surface. The binding is an `@IntoSet` so the shell
 * discovers it without any navigation change (ADR 0007).
 */
class RaymondFeatureDestination @Inject constructor() : FeatureDestination {
    override val id: String = "raymond"
    override val title: String = "Raymond"

    @Composable
    override fun Content(onBack: () -> Unit) {
        RaymondScreen(onBack = onBack)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object RaymondDestinationModule {
    @Provides
    @IntoSet
    fun provideRaymondDestination(destination: RaymondFeatureDestination): FeatureDestination = destination
}
