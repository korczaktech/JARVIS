package com.korczak.morok.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRouterTest {
 private val router=CommandRouter()
 @Test fun statusCommandIsHandled(){assertEquals(CommandResult.Success("Morok está ativo."),router.route("status",CommandSource.TEXT))}
 @Test fun blankCommandFails(){assertTrue(router.route("   ",CommandSource.TEXT) is CommandResult.Failure)}
 @Test fun cameraExecutesWithoutConfirmation(){assertTrue(router.route("abrir câmera",CommandSource.VOICE) is CommandResult.Success)}
 @Test fun naturalLanternVariationsExecute(){for(c in listOf("ligar lanterna","ligar a lanterna","Morok, ligar a lanterna","morok ligar a lanterna","acender a lanterna"))assertTrue(c,router.route(c,CommandSource.VOICE) is CommandResult.Success)}
 @Test fun accentsAndArticlesAreIgnored(){assertTrue(router.route("MOROK, configurações",CommandSource.VOICE) is CommandResult.Success);assertTrue(router.route("abrir o wi-fi",CommandSource.VOICE) is CommandResult.Success)}
 @Test fun percentCommandsParseVariations(){assertEquals(CommandAction.SetVolume(50),(router.route("coloque o volume para 50%",CommandSource.VOICE) as CommandResult.Success).action);assertEquals(CommandAction.SetBrightness(40),(router.route("ajuste o brilho da tela para 40",CommandSource.VOICE) as CommandResult.Success).action)}
 @Test fun sensitiveCallsStillRequireConfirmation(){assertTrue(router.route("Morok, ligar para 5511999999999",CommandSource.VOICE) is CommandResult.RequiresConfirmation)}
}
