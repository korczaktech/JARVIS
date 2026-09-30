package com.korczak.morok.core
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
class CommandRouter {
 fun route(text:String,source:CommandSource):CommandResult {
  var n=normalize(text).replace(Regex("^(?:ok |hey |hello )?morok[,:;.!? ]+"),"").trim().replace(Regex("^(por favor,? |pode |poderia |voce pode |você pode |quero que voce |quero que você )"),"").replace(Regex("( por favor| por gentileza)$"),"").trim()
  if(n.isBlank())return CommandResult.Failure("Diga o comando depois de Morok.")
  fun e(vararg a:String)=a.any{n==normalize(it)}
  return when {
   e("oi","olá","ola","bom dia","boa tarde","boa noite")->CommandResult.Success("Olá. Morok está pronto.")
   e("status","status do sistema","como você está","como voce esta")->CommandResult.Success("Morok está ativo.")
   e("ajuda","comandos","quais comandos","o que você pode fazer","o que voce pode fazer")->CommandResult.Success("Posso controlar brilho, volume, lanterna, conexões, mídia, configurações, câmera, calendário, contatos, arquivos, navegador, pesquisas, chamadas e mensagens.")
   e("cancelar","cancele","pare","parar","pare tudo","stop","cancelar comando")->CommandResult.Success("Execução cancelada.")
   Regex("^(ligar|liga|ligue|acender|acenda|ativar|ative) (a )?lanterna$").matches(n)->CommandResult.Success("Lanterna ligada.",CommandAction.FlashlightOn)
   Regex("^(desligar|desliga|desligue|apagar|apague|desativar|desative) (a )?lanterna$").matches(n)->CommandResult.Success("Lanterna desligada.",CommandAction.FlashlightOff)
   e("abrir configurações","abrir as configurações","abrir configuracoes","configurações","configuracoes","ir para configurações")->CommandResult.Success("Abrindo configurações.",CommandAction.OpenSettings)
   e("wifi","wi-fi","abrir wifi","abrir wi-fi","abrir o wifi","abrir o wi-fi","configurar wifi")->CommandResult.Success("Abrindo configurações de Wi-Fi.",CommandAction.OpenWifiSettings)
   e("bluetooth","abrir bluetooth","abrir o bluetooth","configurar bluetooth")->CommandResult.Success("Abrindo configurações de Bluetooth.",CommandAction.OpenBluetoothSettings)
   e("localização","localizacao","abrir localização","abrir a localização","abrir localizacao")->CommandResult.Success("Abrindo localização.",CommandAction.OpenLocationSettings)
   e("acessibilidade","abrir acessibilidade","abrir a acessibilidade")->CommandResult.Success("Abrindo acessibilidade.",CommandAction.OpenAccessibilitySettings)
   e("configurações de data","configuracoes de data","data")->CommandResult.Success("Abrindo data.",CommandAction.OpenDateSettings)
   e("configurações de hora","configuracoes de hora","hora")->CommandResult.Success("Abrindo hora.",CommandAction.OpenTimeSettings)
   e("abrir câmera","abrir a câmera","abrir camera","abrir a camera","câmera","camera","tirar foto","tirar uma foto")->CommandResult.Success("Abrindo câmera.",CommandAction.OpenCamera)
   e("abrir calendário","abrir o calendário","abrir calendario","abrir o calendario","calendário","calendario")->CommandResult.Success("Abrindo calendário.",CommandAction.OpenCalendar)
   e("abrir contatos","abrir os contatos","contatos","lista de contatos")->CommandResult.Success("Abrindo contatos.",CommandAction.OpenContacts)
   e("abrir arquivos","abrir os arquivos","arquivos","gerenciador de arquivos","meus arquivos")->CommandResult.Success("Abrindo arquivos.",CommandAction.OpenFiles)
   e("notificações","notificacoes","abrir notificações","abrir as notificações","abrir notificacoes")->CommandResult.Success("Abrindo notificações.",CommandAction.OpenNotifications)
   e("configurações do aplicativo","configuracoes do aplicativo","configurações do morok","configuracoes do morok")->CommandResult.Success("Abrindo configurações do Morok.",CommandAction.OpenAppSettings)
   e("volume máximo","volume maximo","aumentar volume ao máximo","aumentar o volume ao máximo")->CommandResult.Success("Volume no máximo.",CommandAction.SetVolume(100))
   e("volume mínimo","volume minimo","diminuir volume ao mínimo","diminuir o volume ao mínimo")->CommandResult.Success("Volume no mínimo.",CommandAction.SetVolume(0))
   Regex("^(defina |definir |coloque |colocar |ajuste |ajustar |aumente |aumentar |diminua |diminuir )?(o )?volume( para| em)? [0-9]{1,3}%?$").matches(n)->parse(n.substringAfter("volume").trim().removePrefix("para").removePrefix("em").trim(),"volume"){CommandAction.SetVolume(it)}
   Regex("^(defina |definir |coloque |colocar |ajuste |ajustar )?(o )?brilho( da tela)?( para)? [0-9]{1,3}%?$").matches(n)->parse(n.substringAfter("brilho").trim().removePrefix("da tela").trim().removePrefix("para").removePrefix("em").trim(),"brilho"){CommandAction.SetBrightness(it)}
   e("aumentar volume","aumente o volume","aumentar o volume","volume mais alto","mais volume")->CommandResult.Success("Aumentando o volume.",CommandAction.VolumeDelta(1))
   e("diminuir volume","diminua o volume","diminuir o volume","volume mais baixo","menos volume")->CommandResult.Success("Diminuindo o volume.",CommandAction.VolumeDelta(-1))
   e("silenciar","silêncio","silencio","modo silencioso","colocar no silencioso","ativar silencioso")->CommandResult.Success("Modo silencioso.",CommandAction.SetRingerMode(CommandAction.RingerMode.SILENT))
   e("vibrar","modo vibratório","modo vibratorio","ativar vibrar")->CommandResult.Success("Modo vibratório.",CommandAction.SetRingerMode(CommandAction.RingerMode.VIBRATE))
   e("som","modo normal","ativar som","tirar do silencioso")->CommandResult.Success("Modo normal.",CommandAction.SetRingerMode(CommandAction.RingerMode.NORMAL))
   e("bateria","status da bateria","quanto de bateria","nível da bateria","nivel da bateria")->CommandResult.Success("Consultando a bateria.",CommandAction.BatteryStatus)
   e("armazenamento","espaço de armazenamento","espaco de armazenamento","quanto espaço tenho","quanto espaco tenho")->CommandResult.Success("Abrindo armazenamento.",CommandAction.StorageSettings)
   e("internet","status da internet","status da rede","rede","conexão","conexao")->CommandResult.Success("Abrindo configurações de rede.",CommandAction.OpenNetworkSettings)
   e("play","pausar","pausar música","pausar musica","continuar","continuar música","continuar musica","reproduzir")->CommandResult.Success("Controlando mídia.",CommandAction.MediaPlayPause)
   e("próxima","proxima","próxima música","proxima musica","música seguinte","musica seguinte")->CommandResult.Success("Próxima mídia.",CommandAction.MediaNext)
   e("anterior","música anterior","musica anterior")->CommandResult.Success("Mídia anterior.",CommandAction.MediaPrevious)
   e("girar tela","girar a tela","rotação automática","rotacao automatica","ativar rotação","ativar rotacao")->CommandResult.Success("Abrindo configurações de tela.",CommandAction.OpenDisplaySettings)
   n.startsWith("ligar para ")||n.startsWith("ligue para ")||n.startsWith("chamar ")->{val v=if(n.startsWith("chamar "))n.removePrefix("chamar ").trim() else n.substringAfter("para ").trim();if(v.isBlank())CommandResult.Failure("Informe o número ou contato.") else CommandResult.RequiresConfirmation("Ligar para $v?",CommandAction.Dial(v))}
   n.startsWith("enviar sms ")||n.startsWith("enviar mensagem ")||n.startsWith("mandar mensagem ")->{val b=n.substringAfter(" ").substringAfter(" ").trim();if(b.isBlank())CommandResult.Failure("Informe a mensagem.") else CommandResult.RequiresConfirmation("Enviar mensagem: $b?",CommandAction.SendSms(null,b))}
   n.startsWith("abrir ")&&(n.contains(".")||n.contains("www."))->CommandResult.Success("Abrindo endereço.",CommandAction.OpenUrl(n.substringAfter(" ").trim().let{if(it.startsWith("http"))it else "https://$it"}))
   n.startsWith("pesquisar ")||n.startsWith("buscar ")||n.startsWith("procure ")||n.startsWith("pesquise ")->{val q=n.substringAfter(" ").trim();if(q.isBlank())CommandResult.Failure("Informe o que deseja pesquisar.") else CommandResult.Success("Pesquisando por $q.",CommandAction.OpenUrl("https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8")))}
   n.startsWith("abrir ")->CommandResult.Failure("Não encontrei esse destino. Diga o nome de um recurso, aplicativo ou endereço.")
   else->CommandResult.Failure("Não reconheci esse comando. Diga ajuda para ver os comandos básicos.")
  }
 }
 private fun normalize(v:String)=Normalizer.normalize(v.lowercase(Locale.ROOT),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").replace(Regex("[!?;]+")," ").replace(Regex("\\s+")," ").trim().removeSuffix(".")
 private fun parse(v:String,label:String,action:(Int)->CommandAction):CommandResult{val x=v.replace("%","").trim().toIntOrNull()?:return CommandResult.Failure("Use $label de 0 a 100.");if(x !in 0..100)return CommandResult.Failure("$label deve ficar entre 0 e 100.");return CommandResult.Success("$label ajustado para $x%.",action(x))}
 fun createCommand(text:String,source:CommandSource)=Command(UUID.randomUUID().toString(),text,source,System.currentTimeMillis())
}