package com.korczak.morok.core
class ToolRegistry{
private val tools=linkedMapOf<String,suspend(ExecutionContext)->CommandResult>()
fun register(name:String,handler:suspend(ExecutionContext)->CommandResult){require(name.isNotBlank());check(name !in tools){"Tool already registered: "+name};tools[name]=handler}
fun contains(name:String)=name in tools
suspend fun execute(name:String,context:ExecutionContext)=tools[name]?.invoke(context)?:CommandResult.Failure("Ferramenta não encontrada: "+name)
}