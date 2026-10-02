package com.korczak.morok.service

import android.Manifest
import android.app.*
import android.content.*
import android.os.*
import android.provider.Settings
import android.speech.*
import androidx.core.app.NotificationCompat
import com.korczak.morok.R
import com.korczak.morok.core.*
import java.text.Normalizer
import java.util.Locale

class MorokForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "morok_assistant"
        const val NOTIFICATION_ID = 1001
        const val ACTION_RESULT = "com.korczak.morok.COMMAND_RESULT"
        const val EXTRA_TEXT = "text"
        const val ACTION_CONFIRM = "com.korczak.morok.CONFIRM"
        const val ACTION_CANCEL = "com.korczak.morok.CANCEL"
        const val ACTION_LISTEN_ONCE = "com.korczak.morok.LISTEN_ONCE"
        fun confirmPending(c: Context) = c.startService(Intent(c, MorokForegroundService::class.java).setAction(ACTION_CONFIRM))
        fun cancelPending(c: Context) = c.startService(Intent(c, MorokForegroundService::class.java).setAction(ACTION_CANCEL))
    }

    private lateinit var pipeline: CommandExecutionPipeline
    private var recognizer: SpeechRecognizer? = null
    private var tts: android.speech.tts.TextToSpeech? = null
    private var listening = false
    private var awaitingCommand = false
    private var restarting = false
    private var generation = 0
    private var partialCommandHandled = false
    private var consecutiveErrors = 0
    private var lastLevelBroadcastAt = 0L
    private var pendingAction: CommandAction? = null

    override fun onCreate() {
        super.onCreate()
        pipeline = CommandExecutionPipeline(CommandRouter(), DeviceCommandExecutor(this), AuditLogger())
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.service_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(NOTIFICATION_ID, notification())
        tts = android.speech.tts.TextToSpeech(this) {
            if (it == android.speech.tts.TextToSpeech.SUCCESS) tts?.language = Locale("pt", "BR")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONFIRM -> pendingAction?.let { action ->
                pendingAction = null
                publishResult(pipeline.executeConfirmed(action, CommandSource.VOICE))
            }
            ACTION_CANCEL -> {
                pendingAction = null
                publishResult(CommandResult.Success("Comando cancelado."))
            }
            ACTION_LISTEN_ONCE -> if (!listening) listenOnce()
            null -> if (!listening) startListening()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        listening = false
        generation++
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
        destroyRecognizer()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Morok pronto — aguardando ativação.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    private fun startListening() {
        if (listening) return
        if (!hasMic()) { broadcast("MIC_STATUS:PERMISSION_DENIED"); return }
        if (!isRecognitionReady()) { broadcast("MIC_STATUS:RECOGNITION_UNAVAILABLE"); return }
        listening = true
        consecutiveErrors = 0
        recreateRecognizer()
        listenNow()
    }

    private fun recreateRecognizer() {
        generation++
        destroyRecognizer()
        val localGeneration = generation
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { r ->
            r.setRecognitionListener(object : RecognitionListener {
                override fun onResults(bundle: Bundle) {
                    if (localGeneration != generation || !listening) return
                    consecutiveErrors = 0
                    val text = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partialCommandHandled) handle(text)
                    partialCommandHandled = false
                    scheduleRestart(150)
                }
                override fun onError(error: Int) {
                    if (localGeneration != generation || !listening) return
                    consecutiveErrors++
                    broadcast("ASR_ERROR:$error:" + errorName(error))
                    scheduleRestart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1000 else 600)
                }
                override fun onReadyForSpeech(params: Bundle?) {
                    if (localGeneration == generation && listening) broadcast("MIC_STATE:READY")
                }
                override fun onBeginningOfSpeech() {
                    if (localGeneration == generation && listening) broadcast("MIC_STATE:SPEECH_START")
                }
                override fun onRmsChanged(value: Float) {
                    if (localGeneration != generation || !listening) return
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastLevelBroadcastAt >= 400) {
                        lastLevelBroadcastAt = now
                        val normalized = ((value.coerceIn(-10f, 10f) + 10f) * 5f).toInt()
                        broadcast("MIC_LEVEL:$normalized:$value")
                    }
                }
                override fun onEndOfSpeech() {
                    if (localGeneration == generation && listening) broadcast("MIC_STATE:SPEECH_END")
                }
                override fun onPartialResults(bundle: Bundle) {
                    if (localGeneration != generation || !listening) return
                    val text = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                    broadcast("ASR_PARTIAL:$text")
                    if (!isWake(text)) return
                    val command = wakeCommand(text)
                    if (command.isNotBlank() && !partialCommandHandled) {
                        partialCommandHandled = true
                        handle(text)
                    } else if (!awaitingCommand) {
                        awaitingCommand = true
                        showOverlay("MOROK\nFale seu comando…")
                        speak("Fale seu comando.")
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {
                    if (localGeneration == generation && buffer != null && buffer.isNotEmpty()) broadcast("MIC_BUFFER:" + buffer.size)
                }
                override fun onEvent(type: Int, params: Bundle?) {}
            })
        }
    }

    private fun scheduleRestart(delay: Long) {
        if (!listening || restarting) return
        restarting = true
        Handler(Looper.getMainLooper()).postDelayed({
            restarting = false
            if (listening) { recreateRecognizer(); listenNow() }
        }, delay)
    }

    private fun destroyRecognizer() {
        recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
        recognizer = null
    }

    private fun listenNow() {
        if (!listening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
        }
        runCatching { recognizer?.startListening(intent) }
            .onFailure { broadcast("ASR_START_ERROR:" + it.javaClass.simpleName); scheduleRestart(1200) }
    }

    private fun listenOnce() {
        if (!hasMic() || !isRecognitionReady()) { broadcast("MIC_STATUS:UNAVAILABLE"); return }
        listening = false
        generation++
        destroyRecognizer()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { r ->
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = broadcast("MIC_STATE:READY")
                override fun onBeginningOfSpeech() = broadcast("MIC_STATE:SPEECH_START")
                override fun onRmsChanged(value: Float) {}
                override fun onPartialResults(bundle: Bundle) {}
                override fun onEndOfSpeech() = broadcast("MIC_STATE:SPEECH_END")
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(type: Int, params: Bundle?) {}
                override fun onResults(bundle: Bundle) {
                    val text = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) { broadcast("ASR_FINAL:$text"); handle(text, true) }
                    destroyRecognizer()
                    stopSelf()
                }
                override fun onError(error: Int) {
                    broadcast("ASR_ERROR:$error:" + errorName(error))
                    destroyRecognizer()
                    stopSelf()
                }
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { recognizer?.startListening(intent) }
            .onFailure { broadcast("ASR_START_ERROR:" + it.javaClass.simpleName); destroyRecognizer(); stopSelf() }
    }

    private fun handle(text: String?, direct: Boolean = false) {
        if (text.isNullOrBlank()) return
        val activeWake = isWake(text)
        if (!direct && !activeWake && !awaitingCommand) return
        val command = if (direct) text.trim() else if (activeWake) wakeCommand(text) else text.trim()
        if (activeWake && command.isBlank()) {
            awaitingCommand = true
            showOverlay("MOROK\nFale seu comando…")
            speak("Fale seu comando.")
            return
        }
        awaitingCommand = false
        showOverlay("MOROK\n$command")
        when (val result = pipeline.handle(command, CommandSource.VOICE)) {
            is CommandResult.RequiresConfirmation -> {
                pendingAction = result.action
                broadcast("CONFIRMATION:" + result.message)
            }
            else -> publishResult(result)
        }
    }

    private fun publishResult(result: CommandResult) {
        val message = when (result) {
            is CommandResult.Success -> result.message
            is CommandResult.Failure -> result.message
            is CommandResult.NeedsPermission -> "Permissão necessária: " + result.permission
            is CommandResult.RequiresConfirmation -> "Confirmação necessária: " + result.message
        }
        broadcast(message)
        if (result !is CommandResult.RequiresConfirmation) speak(message)
    }

    private fun showOverlay(text: String) {
        if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) {
            startService(Intent(this, MorokOverlayService::class.java).putExtra(MorokOverlayService.EXTRA_COMMAND, text))
        }
    }

    private fun speak(text: String) { tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "morok-response") }
    private fun broadcast(text: String) { sendBroadcast(Intent(ACTION_RESULT).putExtra(EXTRA_TEXT, text).setPackage(packageName)) }

    private fun isWake(value: String): Boolean {
        val n = normalizeVoice(value)
        return n.matches(Regex("^(?:ok |hey |hello |ei |e )?(morok|morock|moroque|moroc|morocque|moroke)(?:\\b|$).*"))
    }

    private fun wakeCommand(value: String): String {
        var n = normalizeVoice(value)
        n = n.replaceFirst(Regex("^(?:ok |hey |hello |ei |e )?(morok|morock|moroque|moroc|morocque|moroke)\\s*"), "").trim()
        return n.replaceFirst(Regex("^acorde\\s*"), "").trim()
    }

    private fun normalizeVoice(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun errorName(error: Int) = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "PERMISSION"
        SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "BUSY"
        SpeechRecognizer.ERROR_SERVER -> "SERVER"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "SERVER_DISCONNECTED"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "SPEECH_TIMEOUT"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS"
        else -> "UNKNOWN"
    }

    private fun hasMic() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    private fun isRecognitionReady() = SpeechRecognizer.isRecognitionAvailable(this)
}
