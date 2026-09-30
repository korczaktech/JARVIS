package com.korczak.morok.voice

import android.content.Intent
import android.service.voice.VoiceInteractionService
import com.korczak.morok.service.MorokForegroundService

class MorokVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        setInvocationEffectEnabled(true)
        runCatching { startForegroundService(Intent(this, MorokForegroundService::class.java)) }
    }
}
