package com.korczak.morok.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRouterTest {
    private val router = CommandRouter()

    @Test
    fun statusCommandIsHandled() {
        val result = router.route("status", CommandSource.TEXT)
        assertEquals(CommandResult.Success("Morok está ativo."), result)
    }

    @Test
    fun blankCommandFails() {
        assertTrue(router.route("   ", CommandSource.TEXT) is CommandResult.Failure)
    }

    @Test
    fun openCommandRequiresConfirmation() {
        assertTrue(router.route("abrir câmera", CommandSource.VOICE) is CommandResult.Success)
    }
}
