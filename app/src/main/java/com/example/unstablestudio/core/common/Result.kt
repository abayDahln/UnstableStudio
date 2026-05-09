package com.example.unstablestudio.core.common

/**
 * Sealed interface representing the state of a loadable resource.
 * Provides type-safe state handling without null checks.
 */
sealed interface LoadState {
    data object Idle : LoadState
    data object Loading : LoadState
    data class Success(val data: Any? = null) : LoadState
    data class Error(val message: String, val throwable: Throwable? = null) : LoadState
}

/**
 * Sealed interface representing the result of an operation.
 * Useful for domain operations that can succeed or fail.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun exceptionOrNull(): Throwable? = when (this) {
        is Success -> null
        is Error -> throwable
    }
}

/**
 * Sealed interface for file operation results.
 */
sealed class FileOperationResult {
    data class Success(val message: String = "") : FileOperationResult()
    data class Error(val message: String, val throwable: Throwable? = null) : FileOperationResult()
}
