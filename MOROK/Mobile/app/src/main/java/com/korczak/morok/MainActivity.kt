package com.korczak.morok

import android.Manifest
import android.content.*
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.webkit.*
import android.app.AlertDialog
import android.app.role.RoleManager
import android.app.usage.StorageStatsManager
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.content.pm.PackageManager
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.korczak.morok.core.*
import com.korczak.morok.service.MorokForegroundService
import com.korczak.morok.service.MorokAccessibilityService
import com.korczak.morok.service.MorokOverlayService
import com.korczak.morok.update.UpdateManager
import org.json.JSONObject
import android.content.pm.ApplicationInfo
import java.text.Normalizer
import java.util.Locale

class MainActivity:ComponentActivity(){
 private val router=CommandRouter()
 private lateinit var webView:WebView
 private lateinit var updateManager:UpdateManager
 private val permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){startAssistantService()}
 private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){val m=i.getStringExtra(MorokForegroundService.EXTRA_TEXT)?:return;if(::webView.isInitialized)webView.post{if(m.startsWith("MIC_")||m.startsWith("ASR_")||m.startsWith("AUDIO_LEVEL:")){if(!m.startsWith("AUDIO_LEVEL:")||System.currentTimeMillis()%1000<80)webView.evaluateJavascript("window.MorokNative?.voiceDebug("+JSONObject.quote(m)+");",null)}else {if(m.startsWith("CONFIRMATION:")){val q=m.removePrefix("CONFIRMATION:");AlertDialog.Builder(this@MainActivity).setTitle("Confirmação necessária").setMessage(q).setPositiveButton("CONFIRMAR"){_,_->MorokForegroundService.confirmPending(this@MainActivity)}.setNegativeButton("CANCELAR"){_,_->MorokForegroundService.cancelPending(this@MainActivity)}.show()}else webView.evaluateJavascript("window.MorokNative?.commandResult("+JSONObject.quote(m)+");",null)}}}}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  webView=WebView(this).apply{settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.allowFileAccess=true;settings.allowContentAccess=true;webViewClient=WebViewClient();webChromeClient=WebChromeClient();addJavascriptInterface(NativeBridge(),"MorokNative");setBackgroundColor(android.graphics.Color.BLACK)}
  setContentView(webView);webView.loadUrl("file:///android_asset/frontend/index.html")
  updateManager=UpdateManager(this)
  requestOverlayPermission()
  requestAssistantRole()
  ContextCompat.registerReceiver(this,receiver,IntentFilter(MorokForegroundService.ACTION_RESULT),ContextCompat.RECEIVER_NOT_EXPORTED)
  requestBasePermissions()
 }
 private fun requestBasePermissions(){val p=buildList{add(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=33)add(Manifest.permission.POST_NOTIFICATIONS)};permissionLauncher.launch(p.toTypedArray())}
 private fun startAssistantService(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){return};if(getSharedPreferences("morok",MODE_PRIVATE).getBoolean("auto_updates",true))checkForUpdates()}
 private fun requestOverlayPermission(){if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this))startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:$packageName")))}
 private fun requestAssistantRole(){if(Build.VERSION.SDK_INT>=29){val rm=getSystemService(RoleManager::class.java);if(rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)&&!rm.isRoleHeld(RoleManager.ROLE_ASSISTANT))runCatching{startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),401)}}}
 private fun checkForUpdates(){updateManager.check{r->when{r.startsWith("READY:")->{val p=r.split(":",limit=3);AlertDialog.Builder(this).setTitle("Atualização disponível").setMessage("Morok ${p[1]} está pronta. Atualizar agora?").setPositiveButton("ATUALIZAR"){_,_->updateManager.install(p[2])}.setNegativeButton("AGORA NÃO",null).show()};r.startsWith("ERRO:")->{} }}}
 private fun execute(a:CommandAction){
  try{when(a){
   CommandAction.FlashlightOn,CommandAction.FlashlightOff->{val cm=getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager;val id=cm.cameraIdList.firstOrNull()?:return;cm.setTorchMode(id,a is CommandAction.FlashlightOn)}
   is CommandAction.SetVolume->{val am=getSystemService(AUDIO_SERVICE) as AudioManager;val max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);am.setStreamVolume(AudioManager.STREAM_MUSIC,max*a.percent/100,AudioManager.FLAG_SHOW_UI)}
   is CommandAction.VolumeDelta->{val am=getSystemService(AUDIO_SERVICE) as AudioManager;am.adjustStreamVolume(AudioManager.STREAM_MUSIC,if(a.direction>0)AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI)}
   is CommandAction.SetBrightness->{if(Settings.System.canWrite(this))Settings.System.putInt(contentResolver,Settings.System.SCREEN_BRIGHTNESS,255*a.percent/100)else startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,android.net.Uri.parse("package:$packageName")))}
   is CommandAction.SetRingerMode->{val am=getSystemService(AUDIO_SERVICE) as AudioManager;am.ringerMode=when(a.mode){CommandAction.RingerMode.NORMAL->AudioManager.RINGER_MODE_NORMAL;CommandAction.RingerMode.VIBRATE->AudioManager.RINGER_MODE_VIBRATE;CommandAction.RingerMode.SILENT->AudioManager.RINGER_MODE_SILENT}}
   is CommandAction.OpenUrl->startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(a.url)))
   is CommandAction.OpenApp->openApp(a.query)
   CommandAction.OpenAppList->startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
   CommandAction.AccessibilityBack->if(!MorokAccessibilityService.back())startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
   CommandAction.AccessibilityHome->if(!MorokAccessibilityService.home())startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
   CommandAction.AccessibilityRecents->if(!MorokAccessibilityService.recents())startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
   is CommandAction.AccessibilityClick->if(!MorokAccessibilityService.clickText(a.text)){}
   is CommandAction.AccessibilityType->if(!MorokAccessibilityService.typeText(a.text)){}

   CommandAction.OpenSettings->startActivity(Intent(Settings.ACTION_SETTINGS))
   CommandAction.OpenWifiSettings->startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
   CommandAction.OpenBluetoothSettings->startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
   CommandAction.OpenLocationSettings->startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
   CommandAction.OpenAccessibilitySettings->startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
   CommandAction.OpenAppSettings->startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:$packageName")))
   CommandAction.OpenCamera->startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))
   CommandAction.OpenCalendar->startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
   CommandAction.OpenContacts->startActivity(Intent(Intent.ACTION_VIEW,android.provider.ContactsContract.Contacts.CONTENT_URI))
   CommandAction.OpenFiles->startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE))
   CommandAction.OpenNotifications->startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
   CommandAction.OpenDateSettings->startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
   CommandAction.OpenTimeSettings->startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
   CommandAction.StorageSettings->startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
   CommandAction.OpenNetworkSettings->startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
   CommandAction.OpenDisplaySettings->startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
   CommandAction.MediaPlayPause,CommandAction.MediaNext,CommandAction.MediaPrevious->{val k=when(a){CommandAction.MediaPlayPause->KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;CommandAction.MediaNext->KeyEvent.KEYCODE_MEDIA_NEXT;else->KeyEvent.KEYCODE_MEDIA_PREVIOUS};val am=getSystemService(AUDIO_SERVICE) as AudioManager;am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,k));am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP,k))}
   CommandAction.BatteryStatus,CommandAction.None,is CommandAction.Dial,is CommandAction.SendSms->{}
  }}catch(_:Exception){}
 }
 inner class NativeBridge{
  @JavascriptInterface fun deviceInfo():String{val bm=getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
   val am=getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
   val mi=android.app.ActivityManager.MemoryInfo();am.getMemoryInfo(mi)
   val stat=android.os.StatFs(android.os.Environment.getDataDirectory().path)
   val total=stat.totalBytes;val free=stat.availableBytes;val used=total-free
   return JSONObject().apply{put("battery",bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY));put("sdk",Build.VERSION.SDK_INT);put("model",Build.MODEL);put("appVersion",BuildConfig.VERSION_NAME);put("versionCode",BuildConfig.VERSION_CODE);put("packageName",packageName);put("storageTotal",total);put("storageFree",free);put("storageUsed",used);put("storagePercent",if(total>0)used*100.0/total else 0.0);put("ramTotal",mi.totalMem);put("ramAvailable",mi.availMem);put("ramPercent",if(mi.totalMem>0)(mi.totalMem-mi.availMem)*100.0/mi.totalMem else 0.0)}.toString()}
  @JavascriptInterface fun command(text:String):String{val r=router.route(text,CommandSource.VOICE);when(r){is CommandResult.Success->runOnUiThread{execute(r.action)};is CommandResult.RequiresConfirmation->runOnUiThread{AlertDialog.Builder(this@MainActivity).setTitle("Confirmação necessária").setMessage(r.message).setPositiveButton("CONFIRMAR"){_,_->execute(r.action)}.setNegativeButton("CANCELAR",null).show()};else->{} };val m=when(r){is CommandResult.Success->r.message;is CommandResult.RequiresConfirmation->"Confirmação necessária: "+r.message;is CommandResult.NeedsPermission->"Permissão necessária: "+r.permission;is CommandResult.Failure->r.message};webView.post{webView.evaluateJavascript("window.MorokNative?.commandResult("+JSONObject.quote(m)+");",null)};return m}
  @JavascriptInterface fun microphoneStatus():String=MicrophoneDiagnostics.probe(this@MainActivity)
  @JavascriptInterface fun setAutoUpdate(enabled:Boolean){getSharedPreferences("morok",MODE_PRIVATE).edit().putBoolean("auto_updates",enabled).apply()}
  @JavascriptInterface fun isAccessibilityEnabled():Boolean=MorokAccessibilityService.isEnabled()
  @JavascriptInterface fun startService()=startAssistantService()
  @JavascriptInterface fun stopService()=stopService(Intent(this@MainActivity,MorokForegroundService::class.java))
  @JavascriptInterface fun requestPermissions()=requestBasePermissions()
  @JavascriptInterface fun openAccessibilitySettings(){startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}
  @JavascriptInterface fun openFiles(){startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE))}
  @JavascriptInterface fun requestOverlay(){requestOverlayPermission()}
  @JavascriptInterface fun requestAssistant(){requestAssistantRole()}
  @JavascriptInterface fun startVoice(){if(ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestBasePermissions();return};ContextCompat.startForegroundService(this@MainActivity,Intent(this@MainActivity,MorokForegroundService::class.java));webView.post{webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('morok-voice-start'));",null)}}
  @JavascriptInterface fun installedApps():String{val pm=packageManager;val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);return org.json.JSONArray(pm.queryIntentActivities(intent,0).map{r->JSONObject().apply{put("packageName",r.activityInfo.packageName);put("name",pm.getApplicationLabel(r.activityInfo.applicationInfo).toString())}}).toString()}
  @JavascriptInterface fun openApp(query:String):String{val pm=packageManager;val q=Normalizer.normalize(query.lowercase(Locale.ROOT),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").trim();val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);val match=pm.queryIntentActivities(intent,0).firstOrNull{Normalizer.normalize(pm.getApplicationLabel(it.activityInfo.applicationInfo).toString().lowercase(Locale.ROOT),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").contains(q)}?:return "Aplicativo não encontrado: $query";val launch=pm.getLaunchIntentForPackage(match.activityInfo.packageName)?:return "Aplicativo sem tela inicial: $query";launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);runOnUiThread{startActivity(launch)};return "Abrindo "+pm.getApplicationLabel(match.activityInfo.applicationInfo).toString()}
 }
 private fun openApp(query:String){
  val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  val apps=packageManager.queryIntentActivities(intent,0)
  val q=Normalizer.normalize(query.lowercase(Locale.ROOT),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").trim()
  val match=apps.firstOrNull{Normalizer.normalize(packageManager.getApplicationLabel(it.activityInfo.applicationInfo).toString().lowercase(Locale.ROOT),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").contains(q)}
  match?.let{startActivity(packageManager.getLaunchIntentForPackage(it.activityInfo.packageName))}
 }
 override fun onDestroy(){unregisterReceiver(receiver);webView.removeJavascriptInterface("MorokNative");webView.destroy();super.onDestroy()}
}