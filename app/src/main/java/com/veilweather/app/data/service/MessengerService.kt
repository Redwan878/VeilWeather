package com.veilweather.app.data.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.veilweather.app.data.repository.MessengerStateRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val NOTIFICATION_CHANNEL_ID = "messenger_service"
const val NOTIFICATION_ID = 1

@AndroidEntryPoint
class MessengerService : Service() {

    @Inject
    lateinit var messengerStateRepository: MessengerStateRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Messenger Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background messaging service"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("VeilWeather")
            .setContentText("Messaging service running")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}