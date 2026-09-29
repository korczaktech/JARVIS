package com.korczak.morok.core
import org.junit.Assert.assertTrue
import org.junit.Test
class ExecutionFoundationTest{
@Test fun toolRegistryRejectsDuplicates(){
val registry=ToolRegistry();registry.register("status"){CommandResult.Success("ok")}
var rejected=false
try{registry.register("status"){CommandResult.Success("duplicate")}}catch(_:IllegalStateException){rejected=true}
assertTrue(rejected)
}
}