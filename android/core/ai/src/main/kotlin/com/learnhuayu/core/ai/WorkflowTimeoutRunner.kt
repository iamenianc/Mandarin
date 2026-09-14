package com.learnhuayu.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Runs a workflow HTTP call under its latency budget.
 *
 * The indirection exists for tests: the mock engine answers on real threads while
 * `runTest` uses virtual time, so mock-based request-shape tests inject [NoTimeoutRunner]
 * (no budget). Timeout mapping itself is covered by injecting a fake runner that applies
 * the same [withTimeout] call on the test dispatcher with a virtual delay.
 */
internal interface WorkflowTimeoutRunner {
    suspend fun <T> run(timeoutMillis: Long, block: suspend () -> T): T
}

/** Production runner: enforces the per-workflow budget with [withTimeout] on real time. */
internal object RealTimeoutRunner : WorkflowTimeoutRunner {
    override suspend fun <T> run(timeoutMillis: Long, block: suspend () -> T): T = withContext(Dispatchers.Default) {
        withTimeout(timeoutMillis) { block() }
    }
}

/** Test runner: executes the call with no budget, preserving request-shape assertions. */
internal object NoTimeoutRunner : WorkflowTimeoutRunner {
    override suspend fun <T> run(timeoutMillis: Long, block: suspend () -> T): T = block()
}
