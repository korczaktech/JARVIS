package com.korczak.morok.core
import org.junit.Assert.assertTrue
import org.junit.Test
class BasicCommandsTest{@Test fun commandsExecuteWithoutUnnecessaryConfirmation(){val r=CommandRouter();assertTrue(r.route("abrir câmera",CommandSource.VOICE) is CommandResult.Success);assertTrue(r.route("ligar lanterna",CommandSource.VOICE) is CommandResult.Success);assertTrue(r.route("volume 50",CommandSource.TEXT) is CommandResult.Success);assertTrue(r.route("ligar para 123",CommandSource.VOICE) is CommandResult.RequiresConfirmation)}}