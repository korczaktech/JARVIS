package com.korczak.morok.core

import android.content.Context
import android.media.AudioManager
import android.provider.Settings

class ExecutionVerifier(private val context: Context) {
    fun verify(action: CommandAction, result: ExecutionResult): ExecutionResult {
        if (result !is ExecutionResult.Success) return result
        return try {
            when (action) {
                is CommandAction.SetVolume -> {
                    val am=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val expected=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*action.percent/100
                    val actual=am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (kotlin.math.abs(actual-expected)<=1) result else ExecutionResult.Failure("Volume não confirmou o valor solicitado.")
                }
                is CommandAction.SetBrightness -> {
                    if (!Settings.System.canWrite(context)) result
                    else {
                        val actual=Settings.System.getInt(context.contentResolver,Settings.System.SCREEN_BRIGHTNESS)
                        val expected=255*action.percent/100
                        if (kotlin.math.abs(actual-expected)<=2) result else ExecutionResult.Failure("Brilho não confirmou o valor solicitado.")
                    }
                }
                is CommandAction.SetRingerMode -> {
                    val am=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val expected=when(action.mode){CommandAction.RingerMode.NORMAL->AudioManager.RINGER_MODE_NORMAL;CommandAction.RingerMode.VIBRATE->AudioManager.RINGER_MODE_VIBRATE;CommandAction.RingerMode.SILENT->AudioManager.RINGER_MODE_SILENT}
                    if(am.ringerMode==expected) result else ExecutionResult.Failure("Modo de toque não confirmou o estado solicitado.")
                }
                else -> result
            }
        } catch (e: Exception) {
            ExecutionResult.Failure("Não foi possível verificar o resultado da ação.",e)
        }
    }
}