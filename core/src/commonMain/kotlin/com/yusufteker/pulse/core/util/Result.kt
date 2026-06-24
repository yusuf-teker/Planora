package com.yusufteker.pulse.core.util

/**
 * A sealed wrapper for operation results that follows the Result pattern.
 *
 * Use this to encapsulate success/failure outcomes from repository
 * and use case operations. This prevents exceptions from leaking
 * into the UI layer.
 *
 * @param T The type of the success data
 */
sealed interface Result<out T> {

    /**
     * Represents a successful operation with [data].
     */
    data class Success<T>(val data: T) : Result<T>

    /**
     * Represents a failed operation with an [exception] and optional [message].
     */
    data class Error(
        val exception: Throwable? = null,
        val message: String? = exception?.message
    ) : Result<Nothing>

    /**
     * Represents an in-progress operation.
     */
    data object Loading : Result<Nothing>
}

/**
 * Maps the success data of a [Result] to a new type.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Executes [onSuccess] if this is [Result.Success],
 * [onError] if this is [Result.Error],
 * or [onLoading] if this is [Result.Loading].
 */
inline fun <T> Result<T>.fold(
    onSuccess: (T) -> Unit = {},
    onError: (Throwable?, String?) -> Unit = { _, _ -> },
    onLoading: () -> Unit = {}
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(exception, message)
        is Result.Loading -> onLoading()
    }
}

/**
 * Returns the success data or null if this is not [Result.Success].
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Returns the success data or the [defaultValue] if this is not [Result.Success].
 */
fun <T> Result<T>.getOrDefault(defaultValue: T): T = when (this) {
    is Result.Success -> data
    else -> defaultValue
}
