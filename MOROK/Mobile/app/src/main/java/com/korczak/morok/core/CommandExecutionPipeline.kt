package com.korczak.morok.core

import java.util.UUID

class CommandExecutionPipeline(
    private val router: CommandRouter,
    private val executor: DeviceCommandExecutor,
    private val audit: AuditLogger,
    private val verifier: ExecutionVerifier = ExecutionVerifier(executor.context)
) {
    fun handle(rawText: String, source: CommandSource): CommandResult {
        val command = Command(UUID.randomUUID().toString(), rawText.trim(), source, System.currentTimeMillis())
        audit.commandReceived(command)
        if (command.rawText.isBlank()) {
            return CommandResult.Failure("Comando vazio.").also { audit.result(command, it) }
        }
        val routed = router.route(command.rawText, source)
        if (routed is CommandResult.Success && routed.action != CommandAction.None) {
            return executeAction(routed.action, command)
        }
        audit.result(command, routed)
        return routed
    }

    fun executeConfirmed(action: CommandAction, source: CommandSource = CommandSource.TEXT): CommandResult {
        val command = Command(UUID.randomUUID().toString(), "confirmed:" + action::class.simpleName, source, System.currentTimeMillis())
        audit.commandReceived(command)
        return executeAction(action, command)
    }

    private fun executeAction(action: CommandAction, command: Command): CommandResult {
        val result = when (val execution = verifier.verify(action, executor.execute(action))) {
            is ExecutionResult.Success -> CommandResult.Success(execution.message)
            is ExecutionResult.NeedsPermission -> CommandResult.NeedsPermission(execution.permission)
            is ExecutionResult.Failure -> CommandResult.Failure(execution.message, execution.cause)
        }
        audit.result(command, result)
        return result
    }
}
