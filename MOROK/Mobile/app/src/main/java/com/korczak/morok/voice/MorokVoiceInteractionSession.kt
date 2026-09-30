package com.korczak.morok.voice

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.widget.TextView

class MorokVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    private var text: TextView? = null
    override fun onCreateContentView(): View {
        val v = TextView(context).apply {
            text = "MOROK\nFale seu comando…"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xE6000000.toInt())
                cornerRadius = 48f
                setStroke(2, 0xFF7C3CFF.toInt())
            }
        }
        text = v
        return v
    }
    override fun onShow(args: android.os.Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        text?.text = "MOROK\\nFale seu comando…"
        context.startService(android.content.Intent(context, com.korczak.morok.service.MorokForegroundService::class.java).setAction(com.korczak.morok.service.MorokForegroundService.ACTION_LISTEN_ONCE))
    }
}
