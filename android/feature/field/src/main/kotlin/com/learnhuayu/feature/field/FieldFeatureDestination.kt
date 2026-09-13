package com.learnhuayu.feature.field

import androidx.compose.runtime.Composable
import com.learnhuayu.core.ui.FeatureDestination
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Field: the daily LAMP field loop (FR-30, ADR 0013) - script rehearsal, a field mission with
 * five simulated locals, then a debrief. A feature destination rather than a learning module:
 * it orchestrates module content rather than teaching new content. The binding is an
 * `@IntoSet` so the shell discovers it without any navigation change (ADR 0020).
 */
class FieldFeatureDestination @Inject constructor() : FeatureDestination {
    override val id: String = "field"
    override val title: String = "Field"

    @Composable
    override fun Content(onBack: () -> Unit) {
        FieldScreen(onBack = onBack)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object FieldDestinationModule {
    @Provides
    @IntoSet
    fun provideFieldDestination(destination: FieldFeatureDestination): FeatureDestination = destination
}
