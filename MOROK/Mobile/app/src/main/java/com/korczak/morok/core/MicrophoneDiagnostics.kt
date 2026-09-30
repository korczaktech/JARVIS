package com.korczak.morok.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.sqrt

object MicrophoneDiagnostics {
    fun probe(context: Context, durationMs: Long = 500L): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return "PERMISSION_DENIED"
        }
        val sampleRate = 16000
        val min = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (min <= 0) return "AUDIO_CONFIG_UNAVAILABLE"
        val bufferSize = maxOf(min * 2, 4096)
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (_: Throwable) {
            return "AUDIO_RECORD_CREATE_FAILED"
        }
        return try {
            if (record.state != AudioRecord.STATE_INITIALIZED) return "AUDIO_RECORD_NOT_INITIALIZED"
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) return "AUDIO_RECORD_NOT_RECORDING"
            val samples = ShortArray(1024)
            var total = 0.0
            var count = 0
            val deadline = System.currentTimeMillis() + durationMs
            while (System.currentTimeMillis() < deadline) {
                val n = record.read(samples, 0, samples.size)
                if (n > 0) {
                    for (i in 0 until n) {
                        val v = samples[i].toDouble()
                        total += v * v
                    }
                    count += n
                }
            }
            if (count == 0) "AUDIO_NO_SAMPLES"
            else {
                val rms = sqrt(total / count)
                if (rms <= 1.0) "AUDIO_SILENT:$rms" else "AUDIO_OK:$rms"
            }
        } catch (_: Throwable) {
            "AUDIO_READ_FAILED"
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }
}
