package com.korczak.morok.core
data class ExecutionContext(val command:Command,val requestId:String=command.id,val isOffline:Boolean=false)