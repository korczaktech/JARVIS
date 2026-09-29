package com.korczak.morok.core
class ConfirmationManager {
    fun requiresExplicitConfirmation(result: CommandResult) = result is CommandResult.RequiresConfirmation
}
