package com.korczak.morok.core
import java.util.UUID
class CommandExecutionPipeline(private val router: CommandRouter, private val executor: DeviceCommandExecutor, private val audit: AuditLogger) {
    fun handle(rawText: String, source: CommandSource): CommandResult {
        val command = Command(UUID.randomUUID().toString(), rawText.trim(), source, System.currentTimeMillis())
        audit.commandReceived(command)
        if (command.rawText.isBlank()) return CommandResult.Failure("Comando vazio.").also { audit.result(command, it) }
        val routed = router.route(command.rawText, source)
        if (routed is CommandResult.Success && routed.action != CommandAction.None) {
            val result = when (val execution = executor.execute(routed.action)) {
                is ExecutionResult.Success -> CommandResult.Success(execution.message)
                is ExecutionResult.NeedsPermission -> CommandResult.NeedsPermission(execution.permission)
                is ExecutionResult.Failure -> CommandResult.Failure(execution.message, execution.cause)
            }
            audit.result(command, result)
            return result
        }
        audit.result(command, routed)
        return routed
    }
}