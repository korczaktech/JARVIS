package com.korczak.morok

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.korczak.morok.core.CommandResult
import com.korczak.morok.core.CommandRouter
import com.korczak.morok.core.CommandSource
import com.korczak.morok.service.MorokForegroundService
import org.json.JSONObject
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val router = CommandRouter()
    private lateinit var webView: WebView
    private var speechRecognizer: SpeechRecognizer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (::webView.isInitialized) {
            val granted = results.values.all { it }
            webView.evaluateJavascript("window.MorokNative?.permissionsResult("+granted+");", null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(NativeBridge(), "MorokNative")
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/frontend/index.html")
        requestBasePermissions()
    }

    private fun requestBasePermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startAssistantService() {
        ContextCompat.startForegroundService(this, Intent(this, MorokForegroundService::class.java))
    }

    private fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            notifyUi("Reconhecimento de voz indisponível neste dispositivo.")
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = notifyUi("Escutando…")
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) = notifyUi("Não foi possível reconhecer o comando.")
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        webView.evaluateJavascript("window.MorokNative?.voiceResult("+JSONObject.quote(text)+");", null)
                        command(text)
                    } else {
                        notifyUi("Nenhum comando reconhecido.")
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            recognizer.startListening(intent)
        }
    }

    private fun notifyUi(message: String) {
        if (::webView.isInitialized) {
            webView.post {
                webView.evaluateJavascript("window.MorokNative?.commandResult("+JSONObject.quote(message)+");", null)
            }
        }
    }

    inner class NativeBridge {
        @JavascriptInterface
        fun deviceInfo(): String {
            val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
            val battery = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val intent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val charging = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) in listOf(
                android.os.BatteryManager.BATTERY_STATUS_CHARGING,
                android.os.BatteryManager.BATTERY_STATUS_FULL
            )
            return JSONObject().apply {
                put("battery", battery)
                put("charging", charging)
                put("sdk", Build.VERSION.SDK_INT)
                put("model", Build.MODEL)
            }.toString()
        }

        @JavascriptInterface
        fun command(text: String): String {
            val result = router.route(text, CommandSource.VOICE)
            val message = when (result) {
                is CommandResult.Success -> result.message
                is CommandResult.RequiresConfirmation -> "Confirmação necessária."
                is CommandResult.NeedsPermission -> "Permissão necessária: " + result.permission
                is CommandResult.Failure -> result.message
            }
            notifyUi(message)
            return message
        }

        @JavascriptInterface fun startService() = startAssistantService()
        @JavascriptInterface fun stopService() = stopService(Intent(this@MainActivity, MorokForegroundService::class.java))
        @JavascriptInterface fun requestPermissions() = requestBasePermissions()
        @JavascriptInterface fun startVoice() = runOnUiThread { startVoiceRecognition() }
        @JavascriptInterface fun stopVoice() = speechRecognizer?.stopListening()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        webView.removeJavascriptInterface("MorokNative")
        webView.destroy()
        super.onDestroy()
    }
}