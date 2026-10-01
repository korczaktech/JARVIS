package com.korczak.morok.core
import android.util.Log
class AuditLogger {
 fun commandReceived(command: Command){ Log.i("MorokAudit","command="+command.id+" source="+command.source+" text="+command.rawText) }
 fun result(command: Command,result: CommandResult){ val detail=when(result){is CommandResult.Success->result.message;is CommandResult.Failure->result.message;is CommandResult.NeedsPermission->result.permission;is CommandResult.RequiresConfirmation->result.message}; Log.i("MorokAudit","command="+command.id+" result="+result::class.simpleName+" detail="+detail) }
}