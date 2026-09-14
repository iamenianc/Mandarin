package com.learnhuayu.core.ai

sealed interface WorkflowResult<out T> {
    data class Success<T>(val value: T) : WorkflowResult<T>

    data class Failure(val failure: WorkflowFailure) : WorkflowResult<Nothing>
}

sealed class WorkflowFailure(open val message: String, open val httpStatus: Int?) {
    data class InvalidRequest(override val message: String) : WorkflowFailure(message, 400)

    data class RouteNotFound(override val message: String) : WorkflowFailure(message, 404)

    data class PayloadTooLarge(override val message: String) : WorkflowFailure(message, 413)

    data class ProviderNotConfigured(override val message: String) : WorkflowFailure(message, 501)

    data class UpstreamError(override val message: String) : WorkflowFailure(message, 502)

    data class InvalidUpstreamResponse(override val message: String) : WorkflowFailure(message, 502)

    data class UnexpectedStatus(val status: Int, override val message: String) : WorkflowFailure(message, status)

    data class MalformedResponse(override val message: String) : WorkflowFailure(message, null)

    data class NetworkError(override val message: String, val cause: Throwable? = null) : WorkflowFailure(message, null)

    data class Timeout(
        override val message: String,
        val timeoutMillis: Long? = null,
        val cause: Throwable? = null,
    ) : WorkflowFailure(message, null)

    data class BaseUrlMissing(
        override val message: String = "worker base url is not configured",
    ) : WorkflowFailure(message, null)
}
