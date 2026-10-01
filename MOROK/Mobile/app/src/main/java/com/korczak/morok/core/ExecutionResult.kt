package com.korczak.morok.core
sealed interface ExecutionResult {
    data class Success(val message: String) : ExecutionResult
    data class Failure(val message: String, val cause: Throwable? = null) : ExecutionResult
    data class NeedsPermission(val permission: String, val action: String) : ExecutionResult
}