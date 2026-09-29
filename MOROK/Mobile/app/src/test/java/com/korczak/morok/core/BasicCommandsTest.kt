package com.korczak.morok.core
import org.junit.Assert.assertTrue
import org.junit.Test
class BasicCommandsTest{@Test fun basicCommandsAreRouted(){val r=CommandRouter();assertTrue(r.route("status",CommandSource.TEXT) is CommandResult.Success);assertTrue(r.route("ligar lanterna",CommandSource.TEXT) is CommandResult.Success);assertTrue(r.route("volume 50",CommandSource.TEXT) is CommandResult.Success);assertTrue(r.route("abrir câmera",CommandSource.TEXT) is CommandResult.Success);assertTrue(r.route("pesquisar Android",CommandSource.TEXT) is CommandResult.Success)}}