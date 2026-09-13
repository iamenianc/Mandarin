package com.learnhuayu.app.di

import com.learnhuayu.app.registry.ModuleRegistry
import com.learnhuayu.core.model.LearningModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton
import kotlin.jvm.JvmSuppressWildcards

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LearningModuleMultibindingModule {
    @Multibinds
    abstract fun learningModules(): Set<LearningModule>
}

@Module
@InstallIn(SingletonComponent::class)
internal object ModuleRegistryModule {
    @Provides
    @Singleton
    fun provideModuleRegistry(
        modules: Set<@JvmSuppressWildcards LearningModule>,
    ): ModuleRegistry = ModuleRegistry(modules)
}
