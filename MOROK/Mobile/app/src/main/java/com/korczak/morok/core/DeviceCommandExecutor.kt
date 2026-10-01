package com.korczak.morok.core
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import com.korczak.morok.service.MorokAccessibilityService
import java.text.Normalizer
import java.util.Locale
class DeviceCommandExecutor(private val context: Context) {
    fun execute(action: CommandAction): ExecutionResult = try {
        when (action) {
            CommandAction.None -> ok("Nenhuma ação necessária.")
            CommandAction.FlashlightOn -> torch(true)
            CommandAction.FlashlightOff -> torch(false)
            is CommandAction.SetVolume -> setVolume(action.percent)
            is CommandAction.VolumeDelta -> volumeDelta(action.direction)
            is CommandAction.SetBrightness -> setBrightness(action.percent)
            is CommandAction.SetRingerMode -> setRinger(action.mode)
            is CommandAction.OpenUrl -> open(Intent(Intent.ACTION_VIEW, Uri.parse(action.url)), "Abrindo.")
            is CommandAction.OpenApp -> openApp(action.query)
            CommandAction.OpenAppList -> open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), "Abrindo a tela inicial.")
            CommandAction.AccessibilityBack -> accessibility("Voltar") { MorokAccessibilityService.back() }
            CommandAction.AccessibilityHome -> accessibility("Início") { MorokAccessibilityService.home() }
            CommandAction.AccessibilityRecents -> accessibility("Aplicativos recentes") { MorokAccessibilityService.recents() }
            is CommandAction.AccessibilityClick -> accessibility("Clique em ${action.text}") { MorokAccessibilityService.clickText(action.text) }
            is CommandAction.AccessibilityType -> accessibility("Inserir texto") { MorokAccessibilityService.typeText(action.text) }
            CommandAction.OpenSettings -> settings(Settings.ACTION_SETTINGS, "Abrindo configurações.")
            CommandAction.OpenWifiSettings -> settings(Settings.ACTION_WIFI_SETTINGS, "Abrindo Wi-Fi.")
            CommandAction.OpenBluetoothSettings -> settings(Settings.ACTION_BLUETOOTH_SETTINGS, "Abrindo Bluetooth.")
            CommandAction.OpenLocationSettings -> settings(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Abrindo localização.")
            CommandAction.OpenAccessibilitySettings -> settings(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Abrindo acessibilidade.")
            CommandAction.OpenAppSettings -> settings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "Abrindo configurações do Morok.", Uri.parse("package:${context.packageName}"))
            CommandAction.OpenCamera -> open(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE), "Abrindo câmera.")
            CommandAction.OpenCalendar -> open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR), "Abrindo calendário.")
            CommandAction.OpenContacts -> open(Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI), "Abrindo contatos.")
            CommandAction.OpenFiles -> open(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), "Abrindo arquivos.")
            CommandAction.OpenNotifications -> settings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "Abrindo notificações.")
            CommandAction.OpenDateSettings, CommandAction.OpenTimeSettings -> settings(Settings.ACTION_DATE_SETTINGS, "Abrindo data e hora.")
            CommandAction.StorageSettings -> settings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "Abrindo armazenamento.")
            CommandAction.OpenNetworkSettings -> settings(Settings.ACTION_WIRELESS_SETTINGS, "Abrindo rede.")
            CommandAction.OpenDisplaySettings -> settings(Settings.ACTION_DISPLAY_SETTINGS, "Abrindo tela.")
            CommandAction.MediaPlayPause -> media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Reproduzir/pausar.")
            CommandAction.MediaNext -> media(KeyEvent.KEYCODE_MEDIA_NEXT, "Próxima mídia.")
            CommandAction.MediaPrevious -> media(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Mídia anterior.")
            CommandAction.BatteryStatus -> { val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager; ok("Bateria: ${bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}%.") }
            is CommandAction.Dial -> dial(action.number)
            is CommandAction.SendSms -> sms(action.number, action.body)
        }
    } catch (e: Exception) { ExecutionResult.Failure("Não foi possível executar o comando: ${e.message ?: "erro desconhecido"}", e) }
    private fun ok(message: String) = ExecutionResult.Success(message)
    private fun torch(on: Boolean): ExecutionResult {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val id = cm.cameraIdList.firstOrNull() ?: return ExecutionResult.Failure("Câmera indisponível para controlar a lanterna.")
        cm.setTorchMode(id, on); return ok(if (on) "Lanterna ligada." else "Lanterna desligada.")
    }
    private fun setVolume(percent: Int): ExecutionResult {
        val value = percent.coerceIn(0, 100); val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); am.setStreamVolume(AudioManager.STREAM_MUSIC, max * value / 100, AudioManager.FLAG_SHOW_UI)
        return ok("Volume definido em ${value}%.")
    }
    private fun volumeDelta(direction: Int): ExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (direction >= 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        return ok(if (direction >= 0) "Volume aumentado." else "Volume reduzido.")
    }
    private fun setBrightness(percent: Int): ExecutionResult {
        val value = percent.coerceIn(0, 100)
        if (!Settings.System.canWrite(context)) return ExecutionResult.NeedsPermission("WRITE_SETTINGS", "controle de brilho")
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 255 * value / 100); return ok("Brilho definido em ${value}%.")
    }
    private fun setRinger(mode: CommandAction.RingerMode): ExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.ringerMode = when (mode) { CommandAction.RingerMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL; CommandAction.RingerMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE; CommandAction.RingerMode.SILENT -> AudioManager.RINGER_MODE_SILENT }
        return ok("Modo de toque alterado.")
    }
    private fun accessibility(label: String, operation: () -> Boolean): ExecutionResult {
        if (!MorokAccessibilityService.isEnabled()) return ExecutionResult.NeedsPermission("ACCESSIBILITY", label)
        return if (operation()) ok("${label} executado.") else ExecutionResult.Failure("Não foi possível executar: ${label}.")
    }
    private fun settings(action: String, message: String, data: Uri? = null): ExecutionResult {
        val intent = Intent(action).apply { if (data != null) this.data = data }; return open(intent, message)
    }
    private fun open(intent: Intent, message: String): ExecutionResult {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { context.startActivity(intent); ok(message) } catch (e: Exception) { ExecutionResult.Failure("Não foi possível abrir a tela solicitada.", e) }
    }
    private fun openApp(query: String): ExecutionResult {
        val pm = context.packageManager; val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER); val q = normalize(query)
        val match = pm.queryIntentActivities(launcher, 0).firstOrNull { normalize(pm.getApplicationLabel(it.activityInfo.applicationInfo).toString()).contains(q) }
            ?: return ExecutionResult.Failure("Aplicativo não encontrado: ${query}")
        val launch = pm.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return ExecutionResult.Failure("Aplicativo sem tela inicial: ${query}")
        return open(launch, "Abrindo ${pm.getApplicationLabel(match.activityInfo.applicationInfo)}.")
    }
    private fun media(keyCode: Int, message: String): ExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode)); am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode)); return ok(message)
    }
    private fun dial(number: String): ExecutionResult {
        val clean = number.trim(); if (clean.isBlank()) return ExecutionResult.Failure("Número inválido.")
        return open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(clean)}")), "Abrindo discador.")
    }
    private fun sms(number: String?, body: String): ExecutionResult {
        val clean = number?.trim().orEmpty(); if (clean.isBlank()) return ExecutionResult.Failure("Número inválido.")
        return open(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(clean)}")).apply { putExtra("sms_body", body) }, "Abrindo mensagem.")
    }
    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()
}