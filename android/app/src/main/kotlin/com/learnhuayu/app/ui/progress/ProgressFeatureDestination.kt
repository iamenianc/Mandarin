package com.learnhuayu.app.ui.progress

import androidx.compose.runtime.Composable
import com.learnhuayu.core.ui.FeatureDestination
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Progress: local practice history, recurring feedback and debrief themes, and an audio-first
 * WF-5 summary (FR-34, `docs/03-design.md`). A feature destination rather than a learning
 * module: it reports on practice rather than teaching it. The binding is an `@IntoSet` so the
 * shell discovers it without any navigation change (ADR 0020).
 */
class ProgressFeatureDestination @Inject constructor() : FeatureDestination {
    override val id: String = "progress"
    override val title: String = "Progress"

    @Composable
    override fun Content(onBack: () -> Unit) {
        ProgressScreen(onBack = onBack)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object ProgressDestinationModule {
    @Provides
    @IntoSet
    fun provideProgressDestination(destination: ProgressFeatureDestination): FeatureDestination = destination
}
