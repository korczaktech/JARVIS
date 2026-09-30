package com.korczak.morok.service

import android.Manifest
import android.app.*
import android.content.*
import android.media.AudioManager
import android.os.*
import android.view.KeyEvent
import android.provider.Settings
import android.speech.*
import androidx.core.app.NotificationCompat
import com.korczak.morok.R
import com.korczak.morok.core.*
import java.util.Locale
import java.text.Normalizer

class MorokForegroundService:Service(){
 companion object{const val CHANNEL_ID="morok_assistant";const val NOTIFICATION_ID=1001;const val ACTION_RESULT="com.korczak.morok.COMMAND_RESULT";const val EXTRA_TEXT="text";const val ACTION_CONFIRM="com.korczak.morok.CONFIRM";const val ACTION_CANCEL="com.korczak.morok.CANCEL";const val ACTION_LISTEN_ONCE="com.korczak.morok.LISTEN_ONCE";fun confirmPending(c:Context){c.startService(Intent(c,MorokForegroundService::class.java).setAction(ACTION_CONFIRM))};fun cancelPending(c:Context){c.startService(Intent(c,MorokForegroundService::class.java).setAction(ACTION_CANCEL))}}
 private val router=CommandRouter()
 private var recognizer:SpeechRecognizer?=null
 private var listening=false
 private var awaitingCommand=false
 private var restarting=false
 private var generation=0
 private var lastAudioLevel=0f
 private var lastLevelBroadcastAt=0L
 private var consecutiveErrors=0
 private var partialCommandHandled=false
 private var lastDebugBroadcastAt=0L
 private var tts:android.speech.tts.TextToSpeech?=null
 private var pendingAction:CommandAction?=null
 override fun onCreate(){super.onCreate();getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,getString(R.string.service_channel_name),NotificationManager.IMPORTANCE_LOW));startForeground(NOTIFICATION_ID,notification());tts=android.speech.tts.TextToSpeech(this){if(it==android.speech.tts.TextToSpeech.SUCCESS)tts?.language=Locale("pt","BR")};startListening()}
 override fun onStartCommand(i:Intent?,f:Int,s:Int):Int{if(i?.action==ACTION_LISTEN_ONCE){if(!listening)startListening();listenOnce();return START_STICKY};when(i?.action){ACTION_CONFIRM->{pendingAction?.let{a->pendingAction=null;execute(a);broadcast("Comando confirmado.");speak("Comando confirmado.")}};ACTION_CANCEL->{pendingAction=null;broadcast("Comando cancelado.");speak("Comando cancelado.")}};if(!listening)startListening();return START_STICKY}
 override fun onDestroy(){listening=false;generation++;Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null);destroyRecognizer();tts?.shutdown();tts=null;super.onDestroy()}
 override fun onBind(i:Intent?):IBinder?=null
 private fun notification():Notification=NotificationCompat.Builder(this,CHANNEL_ID).setContentTitle(getString(R.string.app_name)).setContentText("Morok ouvindo “Morok” em segundo plano.").setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build()
 private fun startListening(){
  if(listening)return
  if(!hasMic()){broadcast("Permissão de microfone necessária.");return}
  if(!isRecognitionReady()){broadcast("Reconhecimento de voz indisponível neste dispositivo.");return}
  listening=true
  consecutiveErrors=0
  recreateRecognizer()
  listenNow()
 }
 private fun recreateRecognizer(){
  generation++
  destroyRecognizer()
  val localGeneration=generation
  recognizer=SpeechRecognizer.createSpeechRecognizer(this).also{r->
   r.setRecognitionListener(object:RecognitionListener{
    override fun onResults(b:Bundle){if(localGeneration!=generation||!listening)return;consecutiveErrors=0;val text=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull();if(!partialCommandHandled)handle(text);partialCommandHandled=false;scheduleRestart(150)}
    override fun onError(e:Int){if(localGeneration!=generation||!listening)return;consecutiveErrors++;broadcast("ASR_ERROR:$e:"+errorName(e));scheduleRestart(if(e==SpeechRecognizer.ERROR_RECOGNIZER_BUSY)1000 else 600)}
    override fun onReadyForSpeech(p:Bundle?){if(localGeneration==generation&&listening)broadcast("MIC_OK:Reconhecedor pronto.")}
    override fun onBeginningOfSpeech(){if(localGeneration==generation&&listening)broadcast("MIC_AUDIO:Fala detectada.")}
    override fun onRmsChanged(v:Float){if(localGeneration==generation&&listening){lastAudioLevel=v;val now=SystemClock.elapsedRealtime();if(now-lastLevelBroadcastAt>=300){lastLevelBroadcastAt=now;broadcast("MIC_LEVEL:"+((v.coerceIn(-10f,10f)+10f)*5f).toInt()+":"+v)}}}
    override fun onBufferReceived(b:ByteArray?){if(localGeneration==generation&&listening&&b!=null&&b.isNotEmpty())broadcast("MIC_BUFFER:"+b.size)}
    override fun onEndOfSpeech(){if(localGeneration==generation&&listening)broadcast("MIC_AUDIO:Fim da fala.")}
    override fun onPartialResults(b:Bundle) {
     if (localGeneration != generation || !listening) return
     val text = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
     broadcast("ASR_PARTIAL:$text")
     if (!isWake(text)) return
     val command = wakeCommand(text)
     if (command.isNotBlank() && !partialCommandHandled) {
      partialCommandHandled = true
      handle(text)
     } else {
      awaitingCommand = true
      showOverlay("MOROK\\nFale seu comando…")
      speak("Fale seu comando.")
     }
    }
    override fun onEvent(t:Int,p:Bundle?){}
   })  }
 }
 private fun scheduleRestart(delay:Long){if(!listening||restarting)return;restarting=true;Handler(Looper.getMainLooper()).postDelayed({restarting=false;if(listening){recreateRecognizer();listenNow()}},delay)}
 private fun destroyRecognizer(){recognizer?.let{runCatching{it.cancel()};runCatching{it.destroy()}};recognizer=null}
 private fun listenOnce(){
  if(!hasMic()||!isRecognitionReady())return
  listening=false
  generation++
  destroyRecognizer()
  val localGeneration=generation
  recognizer=SpeechRecognizer.createSpeechRecognizer(this).also{r->
   r.setRecognitionListener(object:RecognitionListener{
    override fun onReadyForSpeech(p:Bundle?){broadcast("MIC_OK:Pronto para falar.")}
    override fun onBeginningOfSpeech(){broadcast("MIC_AUDIO:Fala detectada.")}
    override fun onRmsChanged(v:Float){val now=SystemClock.elapsedRealtime();if(now-lastLevelBroadcastAt>=120){lastLevelBroadcastAt=now;broadcast("MIC_LEVEL:"+((v.coerceIn(-10f,10f)+10f)*5f).toInt())}}
    override fun onPartialResults(b:Bundle){val t=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull();if(!t.isNullOrBlank())broadcast("ASR_PARTIAL:$t")}
    override fun onEndOfSpeech(){broadcast("MIC_AUDIO:Fim da fala.")}
    override fun onResults(b:Bundle){val t=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull();if(!t.isNullOrBlank()){broadcast("ASR_FINAL:$t");handle(t,true)};destroyRecognizer();listening=false;startListening()}
    override fun onError(e:Int){broadcast("ASR_ERROR:$e:"+errorName(e));destroyRecognizer();listening=false;startListening()}
    override fun onBufferReceived(b:ByteArray?){if(b != null && b.isNotEmpty())broadcast("MIC_BUFFER:"+b.size)}
    override fun onEvent(t:Int,p:Bundle?){}
   })
  }
  val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR");putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)}
  runCatching{recognizer?.startListening(intent)}.onFailure{broadcast("ASR_START_ERROR:"+it.javaClass.simpleName);destroyRecognizer();listening=false;startListening()}
 }
 private fun listenNow(){
  if(!listening)return
  val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
   putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
   putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
  }
  try{recognizer?.startListening(i)}catch(e:Throwable){broadcast("ASR_START_ERROR:"+e.javaClass.simpleName);scheduleRestart(1200)}
 }
 private fun errorName(e:Int)=when(e){SpeechRecognizer.ERROR_AUDIO->"AUDIO";SpeechRecognizer.ERROR_CLIENT->"CLIENT";SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->"PERMISSION";SpeechRecognizer.ERROR_NETWORK->"NETWORK";SpeechRecognizer.ERROR_NETWORK_TIMEOUT->"NETWORK_TIMEOUT";SpeechRecognizer.ERROR_NO_MATCH->"NO_MATCH";SpeechRecognizer.ERROR_RECOGNIZER_BUSY->"BUSY";SpeechRecognizer.ERROR_SERVER->"SERVER";SpeechRecognizer.ERROR_SERVER_DISCONNECTED->"SERVER_DISCONNECTED";SpeechRecognizer.ERROR_SPEECH_TIMEOUT->"SPEECH_TIMEOUT";SpeechRecognizer.ERROR_TOO_MANY_REQUESTS->"TOO_MANY_REQUESTS";else->"UNKNOWN"}
 private fun isWake(s:String):Boolean{
  val n=normalizeVoice(s)
  return n.matches(Regex("^(?:ok |hey |hello |ei |e )?(morok|morock|moroque|moroc|morocque|moroke)(?:\\b|$).*"))
 }
 private fun normalizeVoice(s:String)=Normalizer.normalize(s.lowercase(Locale.ROOT),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").replace(Regex("[^a-z0-9 ]")," ").replace(Regex("\\s+")," ").trim()
 private fun wakeCommand(s:String):String{
  var n=normalizeVoice(s)
  n=n.replaceFirst(Regex("^(?:ok |hey |hello |ei |e )?(morok|morock|moroque|moroc|morocque|moroke)\\s*"),"").trim()
  return n.replaceFirst(Regex("^acorde\\s*"),"").trim()
 }
 private fun handle(s:String?,direct:Boolean=false){
  if(s.isNullOrBlank())return
  val activeWake=isWake(s)
  if(!activeWake&&!awaitingCommand)return
  val command=if(activeWake) wakeCommand(s) else s.trim()
  if(activeWake&&command.isBlank()){awaitingCommand=true;showOverlay("MOROK\\nFale seu comando…");return}
  awaitingCommand=false
  showOverlay("MOROK\n"+command)
  if(openInstalledApp(command))return
  val result=router.route(command,CommandSource.VOICE)
  when(result){
   is CommandResult.Success->{execute(result.action);broadcast(result.message);speak(result.message)}
   is CommandResult.RequiresConfirmation->{pendingAction=result.action;broadcast("CONFIRMATION:"+result.message)}
   is CommandResult.Failure->{broadcast(result.message);speak(result.message)}
   is CommandResult.NeedsPermission->broadcast("Permissão necessária: "+result.permission)
  }
 }
 private fun openInstalledApp(command:String):Boolean{
  val n=normalizeVoice(command)
  if(!n.startsWith("abrir "))return false
  val requested=n.removePrefix("abrir ").trim()
  if(requested.isBlank())return false
  val pm=packageManager
  val apps=pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
  val hit=apps.firstOrNull{app->
   val label=normalizeVoice(pm.getApplicationLabel(app).toString())
   label==requested || label.contains(requested) || requested.contains(label)
  } ?: return false
  val intent=pm.getLaunchIntentForPackage(hit.packageName) ?: return false
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
  return try{startActivity(intent);broadcast("Abrindo "+pm.getApplicationLabel(hit).toString()+".");speak("Abrindo "+pm.getApplicationLabel(hit).toString()+".");true}catch(_:Exception){false}
 }
 private fun showOverlay(t:String){if(android.os.Build.VERSION.SDK_INT<23||android.provider.Settings.canDrawOverlays(this))startService(Intent(this,MorokOverlayService::class.java).putExtra(MorokOverlayService.EXTRA_COMMAND,t))}
 private fun speak(t:String){tts?.speak(t,android.speech.tts.TextToSpeech.QUEUE_FLUSH,null,"morok-response")}
 private fun broadcast(t:String){sendBroadcast(Intent(ACTION_RESULT).putExtra(EXTRA_TEXT,t).setPackage(packageName))}
 private fun execute(a:CommandAction){
  try{
   when(a){
    CommandAction.FlashlightOn,CommandAction.FlashlightOff->{
     val cm=getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
     val id=cm.cameraIdList.firstOrNull()?:return
     cm.setTorchMode(id,a is CommandAction.FlashlightOn)
    }
    is CommandAction.SetVolume->{
     val am=getSystemService(AUDIO_SERVICE) as AudioManager
     val max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
     am.setStreamVolume(AudioManager.STREAM_MUSIC,(max*a.percent)/100,AudioManager.FLAG_SHOW_UI)
    }
    is CommandAction.VolumeDelta->{
     val am=getSystemService(AUDIO_SERVICE) as AudioManager
     am.adjustStreamVolume(AudioManager.STREAM_MUSIC,if(a.direction>0)AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI)
    }
    is CommandAction.SetRingerMode->{
     val am=getSystemService(AUDIO_SERVICE) as AudioManager
     am.ringerMode=when(a.mode){CommandAction.RingerMode.NORMAL->AudioManager.RINGER_MODE_NORMAL;CommandAction.RingerMode.VIBRATE->AudioManager.RINGER_MODE_VIBRATE;CommandAction.RingerMode.SILENT->AudioManager.RINGER_MODE_SILENT}
    }
    is CommandAction.OpenUrl->open(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(a.url)))
    is CommandAction.OpenApp->openApp(a.query)
    CommandAction.OpenAppList->openAppList()
    CommandAction.AccessibilityBack->if(!com.korczak.morok.service.MorokAccessibilityService.back())open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    CommandAction.AccessibilityHome->if(!com.korczak.morok.service.MorokAccessibilityService.home())open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    CommandAction.AccessibilityRecents->if(!com.korczak.morok.service.MorokAccessibilityService.recents())open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    is CommandAction.AccessibilityClick->if(!com.korczak.morok.service.MorokAccessibilityService.clickText(a.text))broadcast("Não encontrei esse controle na tela. Ative a Acessibilidade do Morok.")
    is CommandAction.AccessibilityType->if(!com.korczak.morok.service.MorokAccessibilityService.typeText(a.text))broadcast("Não encontrei um campo de texto focado. Ative a Acessibilidade do Morok.")

    CommandAction.OpenSettings->open(Intent(Settings.ACTION_SETTINGS))
    CommandAction.OpenWifiSettings->open(Intent(Settings.ACTION_WIFI_SETTINGS))
    CommandAction.OpenBluetoothSettings->open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    CommandAction.OpenLocationSettings->open(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    CommandAction.OpenAccessibilitySettings->open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    CommandAction.OpenAppSettings->open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:$packageName")))
    CommandAction.OpenCamera->open(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))
    CommandAction.OpenCalendar->open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
    CommandAction.OpenContacts->open(Intent(Intent.ACTION_VIEW,android.provider.ContactsContract.Contacts.CONTENT_URI))
    CommandAction.OpenFiles->open(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE))
    CommandAction.OpenNotifications->open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    CommandAction.OpenDateSettings->open(Intent(Settings.ACTION_DATE_SETTINGS))
    CommandAction.OpenTimeSettings->open(Intent(Settings.ACTION_DATE_SETTINGS))
    CommandAction.StorageSettings->open(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
    CommandAction.OpenNetworkSettings->open(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    CommandAction.OpenDisplaySettings->open(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    CommandAction.MediaPlayPause->sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    CommandAction.MediaNext->sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    CommandAction.MediaPrevious->sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    is CommandAction.SetBrightness->{
     if(Settings.System.canWrite(this)){Settings.System.putInt(contentResolver,Settings.System.SCREEN_BRIGHTNESS,(255*a.percent)/100)}
     else open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,android.net.Uri.parse("package:$packageName")))
    }
    CommandAction.BatteryStatus->broadcast("Bateria: "+(getSystemService(BATTERY_SERVICE) as android.os.BatteryManager).getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)+"%.")
    is CommandAction.Dial, is CommandAction.SendSms, CommandAction.None->{}
   }
  }catch(e:Exception){broadcast("Não foi possível executar o comando: "+(e.message?:"erro desconhecido"))}
 }
 private fun openApp(query:String){
  val pm=packageManager
  val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  val apps=pm.queryIntentActivities(intent,0)
  val q=Normalizer.normalize(query.lowercase(Locale.ROOT),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").trim()
  val match=apps.firstOrNull{Normalizer.normalize(pm.getApplicationLabel(it.activityInfo.applicationInfo).toString().lowercase(Locale.ROOT),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").contains(q)}
  if(match==null){broadcast("Não encontrei o aplicativo: $query");return}
  open(pm.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return)
 }
 private fun openAppList(){
  val i=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  open(i)
 }
 private fun sendMediaKey(k:Int){val am=getSystemService(AUDIO_SERVICE) as AudioManager;am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,k));am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP,k))}
 private fun open(i:Intent){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{startActivity(i)}catch(_:Exception){}}
 private fun hasMic()=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED
 private fun isRecognitionReady()=SpeechRecognizer.isRecognitionAvailable(this)
}