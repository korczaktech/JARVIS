package com.korczak.morok.core
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
class CommandRouter {
 fun route(text:String,source:CommandSource):CommandResult {
  val raw=text.trim()
  val n=raw.lowercase(Locale.ROOT).replace(Regex("\\s+")," ").removeSuffix(".")
  if(n.isBlank()) return CommandResult.Failure("Comando vazio.")
  return when {
   n in setOf("oi","olá","ola","olá morok","ola morok")->CommandResult.Success("Olá. Morok está pronto.")
   n in setOf("status","status do sistema","como você está","como voce esta")->CommandResult.Success("Morok está ativo.")
   n in setOf("ajuda","comandos","o que você pode fazer")->CommandResult.Success("Posso controlar volume, brilho, lanterna, conexões, configurações, apps, câmera, calendário, contatos, arquivos, chamadas, SMS, navegador e pesquisas.")
   n in setOf("cancelar","cancele","pare","parar","stop")->CommandResult.Success("Execução cancelada.")
   n in setOf("configurações","configuracoes","abrir configurações","abrir configuracoes")->CommandResult.Success("Abrindo configurações.",CommandAction.OpenSettings)
   n in setOf("wifi","wi-fi","abrir wifi","abrir wi-fi")->CommandResult.Success("Abrindo configurações de Wi-Fi.",CommandAction.OpenWifiSettings)
   n in setOf("bluetooth","abrir bluetooth")->CommandResult.Success("Abrindo configurações de Bluetooth.",CommandAction.OpenBluetoothSettings)
   n in setOf("localização","localizacao","abrir localização","abrir localizacao")->CommandResult.Success("Abrindo configurações de localização.",CommandAction.OpenLocationSettings)
   n in setOf("acessibilidade","abrir acessibilidade")->CommandResult.Success("Abrindo acessibilidade.",CommandAction.OpenAccessibilitySettings)
   n in setOf("configurações de data","configuracoes de data")->CommandResult.Success("Abrindo data.",CommandAction.OpenDateSettings)
   n in setOf("configurações de hora","configuracoes de hora")->CommandResult.Success("Abrindo hora.",CommandAction.OpenTimeSettings)
   n in setOf("ligar lanterna","acender lanterna")->CommandResult.Success("Lanterna ligada.",CommandAction.FlashlightOn)
   n in setOf("desligar lanterna","apagar lanterna")->CommandResult.Success("Lanterna desligada.",CommandAction.FlashlightOff)
   n.startsWith("volume ")->parsePercent(n.removePrefix("volume "),"volume"){CommandAction.SetVolume(it)}
   n.startsWith("brilho ")->parsePercent(n.removePrefix("brilho "),"brilho"){CommandAction.SetBrightness(it)}
   n in setOf("volume máximo","volume maximo")->CommandResult.Success("Volume no máximo.",CommandAction.SetVolume(100))
   n in setOf("volume mínimo","volume minimo")->CommandResult.Success("Volume no mínimo.",CommandAction.SetVolume(0))
   n in setOf("abrir câmera","abrir camera","câmera","camera")->CommandResult.Success("Abrindo câmera.",CommandAction.OpenCamera)
   n in setOf("abrir calendário","abrir calendario","calendário","calendario")->CommandResult.Success("Abrindo calendário.",CommandAction.OpenCalendar)
   n in setOf("abrir contatos","contatos")->CommandResult.Success("Abrindo contatos.",CommandAction.OpenContacts)
   n in setOf("abrir arquivos","arquivos","gerenciador de arquivos")->CommandResult.Success("Abrindo arquivos.",CommandAction.OpenFiles)
   n in setOf("notificações","notificacoes","abrir notificações","abrir notificacoes")->CommandResult.Success("Abrindo notificações.",CommandAction.OpenNotifications)
   n in setOf("configurações do aplicativo","configuracoes do aplicativo")->CommandResult.Success("Abrindo configurações do Morok.",CommandAction.OpenAppSettings)
   n.startsWith("ligar para ")||n.startsWith("ligue para ")->{
    val number=n.substringAfter("para ").trim()
    if(number.isBlank()) CommandResult.Failure("Informe o número.") else CommandResult.RequiresConfirmation("Ligar para $number?",CommandAction.Dial(number))
   }
   n.startsWith("chamar ")->{
    val number=n.removePrefix("chamar ").trim()
    if(number.isBlank()) CommandResult.Failure("Informe o número.") else CommandResult.RequiresConfirmation("Ligar para $number?",CommandAction.Dial(number))
   }
   n.startsWith("enviar sms ")||n.startsWith("enviar mensagem ")->{
    val body=raw.substringAfter(' ').substringAfter(' ').trim()
    if(body.isBlank()) CommandResult.Failure("Informe a mensagem.") else CommandResult.RequiresConfirmation("Enviar mensagem: $body?",CommandAction.SendSms(null,body))
   }
   n.startsWith("abrir ")&&(n.contains(".")||n.contains("www."))->{
    val u=raw.removePrefix("abrir ").trim().let{if(it.startsWith("http"))it else "https://$it"}
    CommandResult.Success("Abrindo $u.",CommandAction.OpenUrl(u))
   }
   n.startsWith("pesquisar ")||n.startsWith("buscar ")->{
    val q=raw.substringAfter(' ').trim()
    if(q.isBlank()) CommandResult.Failure("Informe o que deseja pesquisar.") else CommandResult.Success("Pesquisando por $q.",CommandAction.OpenUrl("https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8")))
   }
   n.startsWith("abrir ")->CommandResult.Failure("Não encontrei esse destino. Diga o nome de um recurso, aplicativo ou endereço.")
   else->CommandResult.Failure("Não reconheci esse comando. Diga ajuda para ver os comandos básicos.")
  }
 }
 private fun parsePercent(value:String,label:String,action:(Int)->CommandAction):CommandResult {
  val number=value.replace("%","").trim().toIntOrNull()?:return CommandResult.Failure("Use $label de 0 a 100.")
  if(number !in 0..100)return CommandResult.Failure("$label deve ficar entre 0 e 100.")
  return CommandResult.Success("$label ajustado para $number%.",action(number))
 }
 fun createCommand(text:String,source:CommandSource)=Command(UUID.randomUUID().toString(),text,source,System.currentTimeMillis())
}