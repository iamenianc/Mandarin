package com.learnhuayu.app.ui.settings

import androidx.compose.runtime.Composable
import com.learnhuayu.core.ui.FeatureDestination
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Settings: goal, Hangul toggle, recording consent, and one-tap data deletion
 * (`docs/03-design.md`; NFR-4). Settings ships no lessons, so it is a feature
 * destination rather than a learning module; the `@IntoSet` binding lets the shell
 * discover it without a navigation change (ADR 0007).
 */
class SettingsFeatureDestination @Inject constructor() : FeatureDestination {
    override val id: String = "settings"
    override val title: String = "Settings"

    @Composable
    override fun Content(onBack: () -> Unit) {
        SettingsScreen(onBack = onBack)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDestinationModule {
    @Provides
    @IntoSet
    fun provideSettingsDestination(destination: SettingsFeatureDestination): FeatureDestination = destination
}
