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

class MorokForegroundService:Service(){
 companion object{const val CHANNEL_ID="morok_assistant";const val NOTIFICATION_ID=1001;const val ACTION_RESULT="com.korczak.morok.COMMAND_RESULT";const val EXTRA_TEXT="text"}
 private val router=CommandRouter()
 private var recognizer:SpeechRecognizer?=null
 private var listening=false
 private var awaitingCommand=false
 private var tts:android.speech.tts.TextToSpeech?=null
 override fun onCreate(){super.onCreate();getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,getString(R.string.service_channel_name),NotificationManager.IMPORTANCE_LOW));startForeground(NOTIFICATION_ID,notification());tts=android.speech.tts.TextToSpeech(this){if(it==android.speech.tts.TextToSpeech.SUCCESS)tts?.language=Locale("pt","BR")};startListening()}
 override fun onStartCommand(i:Intent?,f:Int,s:Int):Int{if(!listening)startListening();return START_STICKY}
 override fun onDestroy(){listening=false;recognizer?.destroy();recognizer=null;tts?.shutdown();tts=null;super.onDestroy()}
 override fun onBind(i:Intent?):IBinder?=null
 private fun notification():Notification=NotificationCompat.Builder(this,CHANNEL_ID).setContentTitle(getString(R.string.app_name)).setContentText("Morok ouvindo “Morok” em segundo plano.").setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build()
 private fun startListening(){
  if(!hasMic()){broadcast("Permissão de microfone necessária.");return}
  if(!isRecognitionReady()){broadcast("Reconhecimento de voz indisponível neste dispositivo.");return}
  listening=true
  recognizer?.destroy()
  recognizer=SpeechRecognizer.createSpeechRecognizer(this).also{r->
   r.setRecognitionListener(object:RecognitionListener{
    override fun onResults(b:Bundle){handle(b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull());restart()}
    override fun onError(e:Int){restart()}
    override fun onReadyForSpeech(p:Bundle?){}
    override fun onBeginningOfSpeech(){}
    override fun onRmsChanged(v:Float){}
    override fun onBufferReceived(b:ByteArray?){}
    override fun onEndOfSpeech(){}
    override fun onPartialResults(b:Bundle){val s=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull();if(s!=null&&isWake(s)){}}
    override fun onEvent(t:Int,p:Bundle?){}
   })
  }
  listenNow()
 }
 private fun listenNow(){
  if(!listening)return
  val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
   putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
   putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
  }
  try{recognizer?.startListening(i)}catch(_:Exception){restart()}
 }
 private fun restart(){Handler(Looper.getMainLooper()).postDelayed({if(listening)listenNow()},450)}
 private fun isWake(s:String)=Regex("^\\s*(?:ok\\s+|hey\\s+|hello\\s+)?morok\\b",RegexOption.IGNORE_CASE).containsMatchIn(s.trim())
 private fun wakeCommand(s:String)=s.trim().replaceFirst(Regex("^\\s*(?:ok\\s+|hey\\s+|hello\\s+)?morok\\s*(?:,|:|-)?\\s*",RegexOption.IGNORE_CASE),"").replaceFirst(Regex("^acorde\\s*",RegexOption.IGNORE_CASE),"").trim()
 private fun handle(s:String?){
  if(s.isNullOrBlank())return
  val activeWake=isWake(s)
  if(!activeWake&&!awaitingCommand)return
  val command=if(activeWake) wakeCommand(s) else s.trim()
  if(activeWake&&command.isBlank()){awaitingCommand=true;showOverlay("MOROK\\nFale seu comando…");return}
  awaitingCommand=false
  showOverlay("MOROK\n"+command)
  val result=router.route(command,CommandSource.VOICE)
  when(result){
   is CommandResult.Success->{execute(result.action);broadcast(result.message);speak(result.message)}
   is CommandResult.RequiresConfirmation->broadcast("Confirmação necessária: "+result.message)
   is CommandResult.Failure->{broadcast(result.message);speak(result.message)}
   is CommandResult.NeedsPermission->broadcast("Permissão necessária: "+result.permission)
  }
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
 private fun sendMediaKey(k:Int){val am=getSystemService(AUDIO_SERVICE) as AudioManager;am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,k));am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP,k))}
 private fun open(i:Intent){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{startActivity(i)}catch(_:Exception){}}
 private fun hasMic()=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED
 private fun isRecognitionReady()=SpeechRecognizer.isRecognitionAvailable(this)
}