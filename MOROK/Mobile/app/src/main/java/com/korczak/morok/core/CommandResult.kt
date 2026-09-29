package com.korczak.morok.core
sealed interface CommandResult {
 data class Success(val message:String,val action:CommandAction=CommandAction.None):CommandResult
 data class NeedsPermission(val permission:String):CommandResult
 data class RequiresConfirmation(val message:String,val action:CommandAction=CommandAction.None):CommandResult
 data class Failure(val message:String,val cause:Throwable?=null):CommandResult
}