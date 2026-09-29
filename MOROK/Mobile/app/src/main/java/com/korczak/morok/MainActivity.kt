package com.korczak.morok
import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.telephony.PhoneNumberUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.korczak.morok.core.*
import com.korczak.morok.service.MorokForegroundService
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
 private val router=CommandRouter()
 private var status by mutableStateOf("Morok pronto.")
 private var input by mutableStateOf("")
 private var speechRecognizer:SpeechRecognizer?=null
 private var tts:TextToSpeech?=null
 private val permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->status=if(r.values.all{it})"Permissões concedidas." else "Algumas permissões não foram concedidas."}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  tts=TextToSpeech(this){if(it==TextToSpeech.SUCCESS)tts?.language=Locale("pt","BR")}
  requestBasePermissions()
  setContent{
   MaterialTheme{Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text("MOROK",style=MaterialTheme.typography.headlineLarge);Text(status)
    OutlinedTextField(input,{input=it},Modifier.fillMaxWidth(),label={Text("Comando")},singleLine=true)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={execute(input,CommandSource.TEXT)},enabled=input.isNotBlank()){Text("Executar")};OutlinedButton(onClick={ listen() }){Text("Ouvir")}}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={execute("status",CommandSource.BUTTON)}){Text("Status")};OutlinedButton(onClick={execute("ajuda",CommandSource.BUTTON)}){Text("Ajuda")};OutlinedButton(onClick={startAssistantService()}){Text("Serviço")}}
   }}}
  }
 }
 private fun requestBasePermissions(){val p=mutableListOf(Manifest.permission.RECORD_AUDIO);if(android.os.Build.VERSION.SDK_INT>=33)p+=Manifest.permission.POST_NOTIFICATIONS;permissionLauncher.launch(p.toTypedArray())}
 private fun execute(text:String,source:CommandSource){
  when(val result=router.route(text,source)){
   is CommandResult.Success->{status=result.message;speak(result.message);runAction(result.action)}
   is CommandResult.RequiresConfirmation->{status=result.message;speak(result.message);showConfirmation(result.message,result.action)}
   is CommandResult.NeedsPermission->{status="Permissão necessária: ${result.permission}"}
   is CommandResult.Failure->{status=result.message;speak(result.message)}
  }
 }
 private fun showConfirmation(message:String,action:CommandAction){android.app.AlertDialog.Builder(this).setTitle("Confirmar ação").setMessage(message).setNegativeButton("Cancelar",null).setPositiveButton("Confirmar"){_,_->runAction(action)}.show()}
 private fun runAction(action:CommandAction){
  try{when(action){
   CommandAction.None->Unit
   CommandAction.OpenSettings->open(Settings.ACTION_SETTINGS)
   CommandAction.OpenWifiSettings->open(Settings.ACTION_WIFI_SETTINGS)
   CommandAction.OpenBluetoothSettings->open(Settings.ACTION_BLUETOOTH_SETTINGS)
   CommandAction.OpenLocationSettings->open(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
   CommandAction.OpenAccessibilitySettings->open(Settings.ACTION_ACCESSIBILITY_SETTINGS)
   CommandAction.OpenAppSettings->open(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName"))
   CommandAction.OpenDateSettings,CommandAction.OpenTimeSettings->open(Settings.ACTION_DATE_SETTINGS)
   CommandAction.OpenNotifications->open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
   CommandAction.FlashlightOn->setFlashlight(true)
   CommandAction.FlashlightOff->setFlashlight(false)
   is CommandAction.SetVolume->setVolume(action.percent)
   is CommandAction.SetBrightness->setBrightness(action.percent)
   is CommandAction.OpenUrl->open(Intent(Intent.ACTION_VIEW,Uri.parse(action.url)))
   CommandAction.OpenCamera->open(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
   CommandAction.OpenCalendar->open(Intent(Intent.ACTION_VIEW,CalendarContract.CONTENT_URI))
   CommandAction.OpenContacts->open(Intent(Intent.ACTION_VIEW,ContactsContract.Contacts.CONTENT_URI))
   CommandAction.OpenFiles->open(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";addCategory(Intent.CATEGORY_OPENABLE)})
   is CommandAction.Dial->open(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+PhoneNumberUtils.normalizeNumber(action.number))))
   is CommandAction.SendSms->open(Intent(Intent.ACTION_SENDTO).apply{data=Uri.parse("smsto:${action.number ?: ""}");putExtra("sms_body",action.body)})
  }}catch(e:Exception){status="Não foi possível executar: ${e.message ?: "erro desconhecido"}"}
 }
 private fun open(action:String){startActivity(Intent(action))}
 private fun open(action:String,uri:Uri){startActivity(Intent(action,uri))}
 private fun open(intent:Intent){startActivity(intent)}
 private fun setVolume(percent:Int){val am=getSystemService(AudioManager::class.java);val max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);am.setStreamVolume(AudioManager.STREAM_MUSIC,(max*percent/100.0).roundToInt(),0)}
 private fun setBrightness(percent:Int){if(!Settings.System.canWrite(this)){status="Permissão para alterar brilho necessária.";open(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:$packageName"));return};Settings.System.putInt(contentResolver,Settings.System.SCREEN_BRIGHTNESS,(255*percent/100.0).roundToInt())}
 private fun setFlashlight(on:Boolean){val cm=getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager;val id=cm.cameraIdList.firstOrNull{cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)==true}?:throw IllegalStateException("Este aparelho não possui flash.");cm.setTorchMode(id,on)}
 private fun listen(){if(!SpeechRecognizer.isRecognitionAvailable(this)){status="Reconhecimento de voz indisponível.";return};speechRecognizer?.destroy();speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this).also{sr->sr.setRecognitionListener(object:RecognitionListener{
  override fun onReadyForSpeech(p:Bundle?){status="Ouvindo..."};override fun onBeginningOfSpeech(){status="Falando..."};override fun onEndOfSpeech(){};override fun onError(error:Int){status="Não consegui entender o áudio. Código: $error"}
  override fun onResults(results:Bundle?){val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?:return;input=text;execute(text,CommandSource.VOICE)}
  override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){};override fun onPartialResults(b:Bundle?){};override fun onEvent(t:Int,p:Bundle?){}
 });sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR");putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)})}}
 private fun speak(text:String){tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"morok-response")}
 private fun startAssistantService(){ContextCompat.startForegroundService(this,Intent(this,MorokForegroundService::class.java));status="Serviço do Morok iniciado."}
 override fun onDestroy(){speechRecognizer?.destroy();tts?.shutdown();super.onDestroy()}
}