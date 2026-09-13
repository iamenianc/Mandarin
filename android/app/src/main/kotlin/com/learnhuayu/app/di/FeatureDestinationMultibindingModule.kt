package com.learnhuayu.app.di

import com.learnhuayu.core.ui.FeatureDestination
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlin.jvm.JvmSuppressWildcards

/**
 * Declares the multibound set of feature-contributed destinations. Feature modules add
 * their own `@IntoSet` bindings; the shell renders whatever the set exposes (ADR 0007).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class FeatureDestinationMultibindingModule {
    @Multibinds
    abstract fun featureDestinations(): Set<@JvmSuppressWildcards FeatureDestination>
}
