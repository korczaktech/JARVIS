package com.korczak.morok.service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.korczak.morok.R
class MorokForegroundService : Service() {
    companion object { const val CHANNEL_ID = "morok_assistant"; const val NOTIFICATION_ID = 1001 }
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.service_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification()); return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText("Morok está pronto.")
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setOngoing(true)
        .build()
}
