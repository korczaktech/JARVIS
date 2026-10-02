package com.korczak.morok.core

import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

class CommandRouter {
    fun route(text: String, source: CommandSource): CommandResult {
        val n = normalizeCommand(text)
            .replace(Regex("^(?:ok |hey |hello |ei |e )?morok[,:;.!? ]+"), "")
            .replace(Regex("^(por favor,? |pode |poderia |voce pode |quero que voce )"), "")
            .replace(Regex("( por favor| por gentileza)$"), "")
            .trim()

        if (n.isBlank()) return CommandResult.Failure("Diga o comando depois de Morok.")

        fun exact(vararg values: String) = values.any { n == normalizeCommand(it) }

        return when {
            exact("oi", "olá", "bom dia", "boa tarde", "boa noite") ->
                CommandResult.Success("Olá. Morok está pronto.")
            exact("status", "status do sistema", "como você está", "como voce esta") ->
                CommandResult.Success("Morok está ativo.")
            exact("ajuda", "comandos", "quais comandos", "o que você pode fazer", "o que voce pode fazer") ->
                CommandResult.Success("Comandos: lanterna, volume, brilho, som, navegação, aplicativos, configurações, mídia, câmera, calendário, contatos, arquivos, pesquisa, chamadas e mensagens.")
            exact("cancelar", "cancele", "pare", "parar", "pare tudo", "stop", "cancelar comando") ->
                CommandResult.Success("Execução cancelada.")

            Regex("^(ligar|liga|ligue|acender|acenda|ativar|ative) (a )?lanterna$").matches(n) ->
                CommandResult.Success("Lanterna ligada.", CommandAction.FlashlightOn)
            Regex("^(desligar|desliga|desligue|apagar|apague|desativar|desative) (a )?lanterna$").matches(n) ->
                CommandResult.Success("Lanterna desligada.", CommandAction.FlashlightOff)

            exact("abrir configurações", "abrir as configurações", "configurações", "ir para configurações") ->
                CommandResult.Success("Abrindo configurações.", CommandAction.OpenSettings)
            exact("abrir lista de aplicativos", "abrir todos os aplicativos", "mostrar aplicativos", "mostrar todos os aplicativos", "lista de aplicativos") ->
                CommandResult.Success("Abrindo a lista de aplicativos.", CommandAction.OpenAppList)
            exact("voltar", "voltar uma tela", "retroceder") ->
                CommandResult.Success("Voltando.", CommandAction.AccessibilityBack)
            exact("início", "ir para início", "tela inicial", "home") ->
                CommandResult.Success("Indo para a tela inicial.", CommandAction.AccessibilityHome)
            exact("aplicativos recentes", "apps recentes", "abrir recentes", "mostrar recentes", "recentes") ->
                CommandResult.Success("Abrindo aplicativos recentes.", CommandAction.AccessibilityRecents)
            Regex("^(clique|clicar|pressione) em .+").matches(n) ->
                CommandResult.Success("Procurando o controle.", CommandAction.AccessibilityClick(n.substringAfter(" em ").trim()))
            Regex("^(toque|tocar) em .+").matches(n) ->
                CommandResult.Success("Procurando o controle.", CommandAction.AccessibilityClick(n.substringAfter(" em ").trim()))
            Regex("^(digite|escreva|insira) .+").matches(n) ->
                CommandResult.Success("Inserindo texto.", CommandAction.AccessibilityType(n.substringAfter(" ").trim()))

            exact("wifi", "wi-fi", "abrir wifi", "abrir wi-fi", "configurar wifi") ->
                CommandResult.Success("Abrindo configurações de Wi-Fi.", CommandAction.OpenWifiSettings)
            exact("bluetooth", "abrir bluetooth", "configurar bluetooth") ->
                CommandResult.Success("Abrindo configurações de Bluetooth.", CommandAction.OpenBluetoothSettings)
            exact("localização", "abrir localização", "abrir localizacao") ->
                CommandResult.Success("Abrindo localização.", CommandAction.OpenLocationSettings)
            exact("acessibilidade", "abrir acessibilidade") ->
                CommandResult.Success("Abrindo acessibilidade.", CommandAction.OpenAccessibilitySettings)
            exact("configurações de data", "configuracoes de data", "data") ->
                CommandResult.Success("Abrindo data.", CommandAction.OpenDateSettings)
            exact("configurações de hora", "configuracoes de hora", "hora") ->
                CommandResult.Success("Abrindo hora.", CommandAction.OpenTimeSettings)
            exact("abrir câmera", "abrir camera", "câmera", "camera", "tirar foto", "tirar uma foto") ->
                CommandResult.Success("Abrindo câmera.", CommandAction.OpenCamera)
            exact("abrir calendário", "abrir calendario", "calendário", "calendario") ->
                CommandResult.Success("Abrindo calendário.", CommandAction.OpenCalendar)
            exact("abrir contatos", "abrir os contatos", "contatos", "lista de contatos") ->
                CommandResult.Success("Abrindo contatos.", CommandAction.OpenContacts)
            exact("abrir arquivos", "abrir os arquivos", "arquivos", "gerenciador de arquivos", "meus arquivos") ->
                CommandResult.Success("Abrindo arquivos.", CommandAction.OpenFiles)
            exact("notificações", "abrir notificações", "abrir notificacoes") ->
                CommandResult.Success("Abrindo notificações.", CommandAction.OpenNotifications)
            exact("configurações do aplicativo", "configuracoes do aplicativo", "configurações do morok", "configuracoes do morok") ->
                CommandResult.Success("Abrindo configurações do Morok.", CommandAction.OpenAppSettings)

            exact("volume máximo", "volume maximo", "aumentar volume ao máximo", "aumentar o volume ao máximo") ->
                CommandResult.Success("Volume no máximo.", CommandAction.SetVolume(100))
            exact("volume mínimo", "volume minimo", "diminuir volume ao mínimo", "diminuir o volume ao mínimo") ->
                CommandResult.Success("Volume no mínimo.", CommandAction.SetVolume(0))
            parsePercent(n, "volume")?.let { CommandResult.Success("Volume ajustado para $it%.", CommandAction.SetVolume(it)) } != null ->
                parsePercent(n, "volume")!!.let { CommandResult.Success("Volume ajustado para $it%.", CommandAction.SetVolume(it)) }
            parsePercent(n, "brilho")?.let { CommandResult.Success("Brilho ajustado para $it%.", CommandAction.SetBrightness(it)) } != null ->
                parsePercent(n, "brilho")!!.let { CommandResult.Success("Brilho ajustado para $it%.", CommandAction.SetBrightness(it)) }
            exact("aumentar volume", "aumente o volume", "aumentar o volume", "volume mais alto", "mais volume") ->
                CommandResult.Success("Aumentando o volume.", CommandAction.VolumeDelta(1))
            exact("diminuir volume", "diminua o volume", "diminuir o volume", "volume mais baixo", "menos volume") ->
                CommandResult.Success("Diminuindo o volume.", CommandAction.VolumeDelta(-1))
            exact("silenciar", "silêncio", "modo silencioso", "colocar no silencioso", "ativar silencioso") ->
                CommandResult.Success("Modo silencioso.", CommandAction.SetRingerMode(CommandAction.RingerMode.SILENT))
            exact("vibrar", "modo vibratório", "modo vibratorio", "ativar vibrar") ->
                CommandResult.Success("Modo vibratório.", CommandAction.SetRingerMode(CommandAction.RingerMode.VIBRATE))
            exact("som", "modo normal", "ativar som", "tirar do silencioso") ->
                CommandResult.Success("Modo normal.", CommandAction.SetRingerMode(CommandAction.RingerMode.NORMAL))

            exact("play", "pausar", "pausar música", "pausar musica", "continuar", "continuar música", "continuar musica", "reproduzir") ->
                CommandResult.Success("Controlando mídia.", CommandAction.MediaPlayPause)
            exact("próxima", "proxima", "próxima música", "proxima musica", "música seguinte", "musica seguinte") ->
                CommandResult.Success("Próxima mídia.", CommandAction.MediaNext)
            exact("anterior", "música anterior", "musica anterior") ->
                CommandResult.Success("Mídia anterior.", CommandAction.MediaPrevious)
            exact("girar tela", "girar a tela", "rotação automática", "rotacao automatica", "ativar rotação", "ativar rotacao") ->
                CommandResult.Success("Abrindo configurações de tela.", CommandAction.OpenDisplaySettings)

            exact("bateria", "status da bateria", "quanto de bateria", "nível da bateria", "nivel da bateria") ->
                CommandResult.Success("Consultando a bateria.", CommandAction.BatteryStatus)
            exact("armazenamento", "espaço de armazenamento", "quanto espaço tenho", "quanto espaco tenho") ->
                CommandResult.Success("Abrindo armazenamento.", CommandAction.StorageSettings)
            exact("internet", "status da internet", "status da rede", "rede", "conexão", "conexao") ->
                CommandResult.Success("Abrindo configurações de rede.", CommandAction.OpenNetworkSettings)

            n.startsWith("ligar para ") || n.startsWith("ligue para ") || n.startsWith("chamar ") -> {
                val value = when {
                    n.startsWith("chamar ") -> n.removePrefix("chamar ").trim()
                    else -> n.substringAfter("para ").trim()
                }
                if (value.isBlank()) CommandResult.Failure("Informe o número ou contato.")
                else CommandResult.RequiresConfirmation("Ligar para $value?", CommandAction.Dial(value))
            }
            n.startsWith("enviar sms") || n.startsWith("enviar mensagem") || n.startsWith("mandar mensagem") -> {
                parseSms(n)
            }

            n.startsWith("abrir ") && (n.contains(".") || n.contains("www.")) ->
                CommandResult.Success("Abrindo endereço.", CommandAction.OpenUrl(n.substringAfter(" ").let { if (it.startsWith("http")) it else "https://$it" }))
            n.startsWith("pesquisar ") || n.startsWith("buscar ") || n.startsWith("procure ") || n.startsWith("pesquise ") -> {
                val q = n.substringAfter(" ").trim()
                if (q.isBlank()) CommandResult.Failure("Informe o que deseja pesquisar.")
                else CommandResult.Success("Pesquisando por $q.", CommandAction.OpenUrl("https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8")))
            }
            n.startsWith("abrir ") -> CommandResult.Success("Procurando o aplicativo.", CommandAction.OpenApp(n.substringAfter(" ").trim()))
            else -> CommandResult.Failure("Não reconheci esse comando. Diga ajuda para ver os comandos básicos.")
        }
    }

    private fun parseSms(n: String): CommandResult {
        val match = Regex("^(?:enviar sms|enviar mensagem|mandar mensagem)(?: para)? ([+0-9() -]{8,})(?: dizendo| com a mensagem|:)(.+)$").find(n)
        if (match != null) {
            val number = match.groupValues[1].filter { it.isDigit() || it == '+' }
            val body = match.groupValues[2].trim()
            if (number.length < 8 || body.isBlank()) return CommandResult.Failure("Informe número e mensagem.")
            return CommandResult.RequiresConfirmation("Enviar mensagem para $number: $body?", CommandAction.SendSms(number, body))
        }
        return CommandResult.Failure("Use: enviar mensagem para número dizendo texto.")
    }

    private fun parsePercent(n: String, label: String): Int? {
        val pattern = when (label) {
            "volume" -> Regex("^(?:defina |definir |coloque |colocar |ajuste |ajustar |aumente |aumentar |diminua |diminuir )?(?:o )?volume(?: para| em)? ([0-9]{1,3})%?$")
            else -> Regex("^(?:defina |definir |coloque |colocar |ajuste |ajustar )?(?:o )?brilho(?: da tela)?(?: para| em)? ([0-9]{1,3})%?$")
        }
        val raw = pattern.matchEntire(n)?.groupValues?.get(1) ?: return null
        val value = raw.toIntOrNull() ?: return null
        return value.takeIf { it in 0..100 }
    }

    private fun normalizeCommand(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[!?;]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removeSuffix(".")

    fun createCommand(text: String, source: CommandSource) =
        Command(UUID.randomUUID().toString(), text, source, System.currentTimeMillis())
}
