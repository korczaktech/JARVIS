package com.korczak.morok.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class MorokOverlayService : Service() {
    private var window: WindowManager? = null
    private var view: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        show(intent?.getStringExtra(EXTRA_COMMAND) ?: "MOROK\nEstou ouvindo…")
        return START_NOT_STICKY
    }

    private fun show(text: String) {
        if (!Settings.canDrawOverlays(this)) return
        handler.post {
            remove()
            val tv = TextView(this).apply {
                this.text = text
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(42, 28, 42, 28)
                background = GradientDrawable().apply {
                    setColor(0xEE05060D.toInt())
                    cornerRadius = 42f
                    setStroke(2, 0xFF8B45FF.toInt())
                }
                elevation = 18f
            }
            val type = if (android.os.Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 72
                width = (resources.displayMetrics.widthPixels * .92f).toInt()
            }
            window = getSystemService(WINDOW_SERVICE) as WindowManager
            view = tv
            runCatching { window?.addView(tv, lp) }
            handler.postDelayed({ remove() }, 6500)
        }
    }

    private fun remove() {
        view?.let { runCatching { window?.removeView(it) } }
        view = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        remove()
        super.onDestroy()
    }

    companion object { const val EXTRA_COMMAND = "command" }
}
