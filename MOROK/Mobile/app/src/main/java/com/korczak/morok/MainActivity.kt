package com.korczak.morok

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    private val router = CommandRouter()
    private lateinit var webView: WebView
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        webView.evaluateJavascript("window.MorokNative?.permissionsResult("+results.values.all { it }+");", null)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBasePermissions()
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
    inner class NativeBridge {
        @JavascriptInterface fun deviceInfo(): String {
            val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
            val battery = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val intent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val charging = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) in listOf(
                android.os.BatteryManager.BATTERY_STATUS_CHARGING,
                android.os.BatteryManager.BATTERY_STATUS_FULL
            )
            return JSONObject().apply { put("battery", battery); put("charging", charging); put("sdk", Build.VERSION.SDK_INT); put("model", Build.MODEL) }.toString()
        }
        @JavascriptInterface fun command(text: String): String {
            val result = router.route(text, CommandSource.VOICE)
            val message = when (result) {
                is CommandResult.Success -> result.message
                is CommandResult.RequiresConfirmation -> "Confirmação necessária."
                is CommandResult.NeedsPermission -> "Permissão necessária: " + result.permission
                is CommandResult.Failure -> result.message
            }
            webView.post { webView.evaluateJavascript("window.MorokNative?.commandResult("+JSONObject.quote(message)+");", null) }
            return message
        }
        @JavascriptInterface fun startService() = startAssistantService()
        @JavascriptInterface fun stopService() = stopService(Intent(this@MainActivity, MorokForegroundService::class.java))
        @JavascriptInterface fun requestPermissions() = requestBasePermissions()
        @JavascriptInterface fun startVoice() { runOnUiThread { webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('morok-voice-start'));", null) } }
    }
    override fun onDestroy() {
        webView.removeJavascriptInterface("MorokNative")
        webView.destroy()
        super.onDestroy()
    }
}