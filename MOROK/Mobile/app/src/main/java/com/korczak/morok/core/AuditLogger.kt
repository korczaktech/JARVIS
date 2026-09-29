package com.korczak.morok.core
import android.util.Log
class AuditLogger {
 fun commandReceived(command: Command){ Log.i("MorokAudit","command=${command.id} source=${command.source}") }
 fun result(command: Command,result: CommandResult){ Log.i("MorokAudit","command=${command.id} result=${result::class.simpleName}") }
}