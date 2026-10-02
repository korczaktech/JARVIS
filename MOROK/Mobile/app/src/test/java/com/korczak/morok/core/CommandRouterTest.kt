package com.korczak.morok.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRouterTest {
    private val router = CommandRouter()
    private fun result(text: String) = router.route(text, CommandSource.VOICE)

    @Test fun greetingAndHelpAreHandled() {
        assertTrue(result("oi") is CommandResult.Success)
        assertTrue(result("ajuda") is CommandResult.Success)
    }

    @Test fun wakeAndAccentNormalizationWork() {
        assertTrue(result("MOROK, configurações") is CommandResult.Success)
        assertTrue(result("Morok, ligar a lanterna") is CommandResult.Success)
        assertTrue(result("ok morok, abrir o wi-fi") is CommandResult.Success)
    }

    @Test fun flashlightAliasesWork() {
        assertTrue(result("ligar lanterna") is CommandResult.Success)
        assertTrue(result("acender a lanterna") is CommandResult.Success)
        assertTrue(result("desligar lanterna") is CommandResult.Success)
    }

    @Test fun completeAccessibilityCommandsMapToActions() {
        assertEquals(CommandAction.AccessibilityLongClick("configuracoes"), (result("pressione e segure em Configurações") as CommandResult.Success).action)
        assertEquals(CommandAction.SwipeDirection.LEFT, ((result("deslize para a esquerda") as CommandResult.Success).action as CommandAction.AccessibilitySwipe).direction)
        assertEquals(CommandAction.SwipeDirection.RIGHT, ((result("deslize para a direita") as CommandResult.Success).action as CommandAction.AccessibilitySwipe).direction)
        assertEquals(CommandAction.SwipeDirection.UP, ((result("deslize para cima") as CommandResult.Success).action as CommandAction.AccessibilitySwipe).direction)
        assertEquals(CommandAction.SwipeDirection.DOWN, ((result("deslize para baixo") as CommandResult.Success).action as CommandAction.AccessibilitySwipe).direction)
        assertEquals(CommandAction.ScrollDirection.UP, ((result("role para cima") as CommandResult.Success).action as CommandAction.AccessibilityScroll).direction)
        assertEquals(CommandAction.ScrollDirection.DOWN, ((result("role para baixo") as CommandResult.Success).action as CommandAction.AccessibilityScroll).direction)
        assertTrue(result("fechar aplicativo") is CommandResult.Success)
        assertTrue(result("ler a tela") is CommandResult.Success)
        assertTrue(result("abrir permissões") is CommandResult.Success)
    }

    @Test fun navigationCommandsMapToActions() {
        assertEquals(CommandAction.AccessibilityBack, (result("voltar") as CommandResult.Success).action)
        assertEquals(CommandAction.AccessibilityHome, (result("tela inicial") as CommandResult.Success).action)
        assertEquals(CommandAction.AccessibilityRecents, (result("apps recentes") as CommandResult.Success).action)
        assertTrue(result("clique em Configurações") is CommandResult.Success)
        assertTrue(result("toque em Continuar") is CommandResult.Success)
        assertTrue(result("tape em Continuar") is CommandResult.Success)
        assertTrue(result("digite Korczak") is CommandResult.Success)
    }

    @Test fun volumeAndBrightnessCommandsAreValidated() {
        assertEquals(CommandAction.SetVolume(50), (result("coloque o volume para 50%") as CommandResult.Success).action)
        assertEquals(CommandAction.SetBrightness(40), (result("ajuste o brilho da tela para 40") as CommandResult.Success).action)
        assertTrue(result("volume 101") is CommandResult.Failure)
        assertTrue(result("brilho 101") is CommandResult.Failure)
    }

    @Test fun mediaAndSoundCommandsWork() {
        assertEquals(CommandAction.MediaPlayPause, (result("pausar música") as CommandResult.Success).action)
        assertEquals(CommandAction.MediaNext, (result("próxima música") as CommandResult.Success).action)
        assertEquals(CommandAction.MediaPrevious, (result("música anterior") as CommandResult.Success).action)
        assertEquals(CommandAction.RingerMode.SILENT, ((result("silenciar") as CommandResult.Success).action as CommandAction.SetRingerMode).mode)
        assertEquals(CommandAction.RingerMode.VIBRATE, ((result("vibrar") as CommandResult.Success).action as CommandAction.SetRingerMode).mode)
        assertEquals(CommandAction.RingerMode.NORMAL, ((result("modo normal") as CommandResult.Success).action as CommandAction.SetRingerMode).mode)
    }

    @Test fun settingsAndDeviceCommandsWork() {
        assertTrue(result("abrir câmera") is CommandResult.Success)
        assertTrue(result("abrir calendário") is CommandResult.Success)
        assertTrue(result("abrir contatos") is CommandResult.Success)
        assertTrue(result("abrir arquivos") is CommandResult.Success)
        assertTrue(result("abrir acessibilidade") is CommandResult.Success)
        assertTrue(result("abrir localização") is CommandResult.Success)
        assertTrue(result("abrir bluetooth") is CommandResult.Success)
        assertTrue(result("abrir notificações") is CommandResult.Success)
        assertTrue(result("abrir configurações do Morok") is CommandResult.Success)
        assertTrue(result("bateria") is CommandResult.Success)
        assertTrue(result("armazenamento") is CommandResult.Success)
        assertTrue(result("internet") is CommandResult.Success)
    }

    @Test fun appAndWebCommandsWork() {
        assertEquals(CommandAction.OpenApp("chrome"), (result("abrir chrome") as CommandResult.Success).action)
        assertEquals(CommandAction.OpenUrl("https://example.com"), (result("abrir example.com") as CommandResult.Success).action)
        assertTrue(result("pesquisar notícias sobre tecnologia") is CommandResult.Success)
    }

    @Test fun sensitiveCallsAndMessagesRequireConfirmation() {
        val call = result("ligar para 5511999999999") as CommandResult.RequiresConfirmation
        assertTrue(call.message.contains("5511999999999"))
        val sms = result("enviar mensagem para 5511999999999 dizendo olá") as CommandResult.RequiresConfirmation
        assertEquals(CommandAction.SendSms("5511999999999", "ola"), sms.action)
    }

    @Test fun unknownAndBlankCommandsFail() {
        assertTrue(result("   ") is CommandResult.Failure)
        assertTrue(result("fazer uma coisa que não existe") is CommandResult.Failure)
    }
}
