package com.veilweather.app.data.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.veilweather.app.R
import com.veilweather.app.data.repository.MessengerStateRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.concurrent.thread

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
        
        // Service is running - WebSocket connection would be established here
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        ).apply {
            setName("Messenger Service")
            setDescription("Background messaging service")
        }.build()

        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("VeilWeather")
            .setContentText("Messaging service running")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}