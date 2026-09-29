package com.korczak.morok.core
import java.util.Locale
import java.util.UUID
class CommandRouter {
    fun route(text: String, source: CommandSource): CommandResult {
        val normalized = text.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return CommandResult.Failure("Comando vazio.")
        return when {
            normalized == "status" || normalized == "status do sistema" -> CommandResult.Success("Morok está ativo.")
            normalized == "cancelar" || normalized == "pare" -> CommandResult.Success("Execução cancelada.")
            normalized.startsWith("abrir ") -> CommandResult.RequiresConfirmation("Abrir aplicativo solicitado: ${normalized.removePrefix("abrir ").trim()}")
            else -> CommandResult.Failure("Comando ainda não cadastrado: $normalized")
        }
    }
    fun createCommand(text: String, source: CommandSource) = Command(
        UUID.randomUUID().toString(), text, source, System.currentTimeMillis()
    )
}
