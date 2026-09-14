package com.learnhuayu.core.ai

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton
import kotlin.jvm.JvmSuppressWildcards

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WorkerWorkflowModule {
    @Multibinds
    abstract fun workerHttpConfigs(): Set<@JvmSuppressWildcards WorkerHttpConfig>

    @Multibinds
    abstract fun workflowTimeoutConfigs(): Set<@JvmSuppressWildcards WorkflowTimeouts>

    @Binds
    abstract fun bindPronunciationFeedbackWorkflow(workflows: WorkerWorkflows): PronunciationFeedbackWorkflow

    @Binds
    abstract fun bindConversationTurnWorkflow(workflows: WorkerWorkflows): ConversationTurnWorkflow

    @Binds
    abstract fun bindSpeechSynthesisWorkflow(workflows: WorkerWorkflows): SpeechSynthesisWorkflow

    @Binds
    abstract fun bindResponseTranscriptionWorkflow(workflows: WorkerWorkflows): ResponseTranscriptionWorkflow

    @Binds
    abstract fun bindProgressSummaryWorkflow(workflows: WorkerWorkflows): ProgressSummaryWorkflow

    @Binds
    abstract fun bindMandarinQaWorkflow(workflows: WorkerWorkflows): MandarinQaWorkflow

    @Binds
    abstract fun bindExerciseGenerationWorkflow(workflows: WorkerWorkflows): ExerciseGenerationWorkflow

    @Binds
    abstract fun bindFieldMissionGenerationWorkflow(workflows: WorkerWorkflows): FieldMissionGenerationWorkflow

    @Binds
    abstract fun bindLocalTurnWorkflow(workflows: WorkerWorkflows): LocalTurnWorkflow
}

@Module
@InstallIn(SingletonComponent::class)
internal object WorkerHttpModule {
    @Provides
    @Singleton
    fun provideWorkerHttpConfig(configs: Set<@JvmSuppressWildcards WorkerHttpConfig>): WorkerHttpConfig = configs.firstOrNull() ?: WorkerHttpConfig()

    @Provides
    @Singleton
    fun provideWorkerHttpClient(config: WorkerHttpConfig): HttpClient = workerHttpClient(OkHttp.create(), config)

    @Provides
    @Singleton
    fun provideWorkflowTimeouts(configs: Set<@JvmSuppressWildcards WorkflowTimeouts>): WorkflowTimeouts = configs.firstOrNull() ?: WorkflowTimeouts()

    @Provides
    @Singleton
    fun provideWorkflowTimeoutRunner(): WorkflowTimeoutRunner = RealTimeoutRunner

    @Provides
    @Singleton
    fun provideWorkerWorkflows(
        client: HttpClient,
        config: WorkerHttpConfig,
        timeouts: WorkflowTimeouts,
        timeoutRunner: WorkflowTimeoutRunner,
    ): WorkerWorkflows = WorkerWorkflows(client, config, timeouts, timeoutRunner)
}
