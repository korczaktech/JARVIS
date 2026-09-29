package com.korczak.morok.core
sealed interface CommandResult {
    data class Success(val message: String) : CommandResult
    data class NeedsPermission(val permission: String) : CommandResult
    data class RequiresConfirmation(val message: String) : CommandResult
    data class Failure(val message: String, val cause: Throwable? = null) : CommandResult
}
