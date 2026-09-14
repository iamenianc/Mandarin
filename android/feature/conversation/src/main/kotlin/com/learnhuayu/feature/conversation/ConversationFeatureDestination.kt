package com.learnhuayu.feature.conversation

import androidx.compose.runtime.Composable
import com.learnhuayu.core.ui.FeatureDestination
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Conversation: the coaching conversation surface (FR-11, WF-2). A feature destination
 * rather than a learning module: it adapts difficulty and corrects gently instead of
 * teaching fixed lessons. The binding is an `@IntoSet` so the shell discovers it without
 * any navigation change (ADR 0020).
 */
class ConversationFeatureDestination @Inject constructor() : FeatureDestination {
    override val id: String = "conversation"
    override val title: String = "Conversation"

    @Composable
    override fun Content(onBack: () -> Unit) {
        ConversationScreen(onBack = onBack)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object ConversationDestinationModule {
    @Provides
    @IntoSet
    fun provideConversationDestination(destination: ConversationFeatureDestination): FeatureDestination = destination
}
