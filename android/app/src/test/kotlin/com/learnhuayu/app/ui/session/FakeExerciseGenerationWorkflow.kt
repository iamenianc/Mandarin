package com.learnhuayu.app.ui.session

import com.learnhuayu.core.ai.ExerciseGenerationRequest
import com.learnhuayu.core.ai.ExerciseGenerationWorkflow
import com.learnhuayu.core.ai.GeneratedExercises
import com.learnhuayu.core.ai.WorkflowResult

class FakeExerciseGenerationWorkflow(
    private var result: WorkflowResult<GeneratedExercises> =
        WorkflowResult.Success(GeneratedExercises()),
    private var failure: Throwable? = null,
) : ExerciseGenerationWorkflow {

    val requests = mutableListOf<ExerciseGenerationRequest>()

    fun returns(result: WorkflowResult<GeneratedExercises>) {
        this.result = result
        this.failure = null
    }

    fun throws(failure: Throwable) {
        this.failure = failure
    }

    override suspend fun generate(request: ExerciseGenerationRequest): WorkflowResult<GeneratedExercises> {
        requests += request
        failure?.let { throw it }
        return result
    }
}
