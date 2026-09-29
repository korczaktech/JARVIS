package com.korczak.morok.core
import kotlinx.coroutines.*
class TaskManager{
private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
fun launch(block:suspend()->Unit):Job=scope.launch{block()}
fun cancelAll(){scope.coroutineContext[Job]?.cancel()}
}