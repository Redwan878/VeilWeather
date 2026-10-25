package com.veilweather.app.data.service

import android.app.*
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.veilweather.app.R

class MessagingPushService {

    companion object {
        private const val CHANNEL_ID = "push_notifications"
        private const val NOTIFICATION_ID = 2

        fun showIncomingMessageNotification(context: android.content.Context, sender: String, message: String) {
            createChannel(context)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("New Message")
                .setContentText("From: $sender")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        fun showCallNotification(context: android.content.Context, caller: String, isVideo: Boolean) {
            createChannel(context)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Incoming Call")
            if (isVideo) {
                notification.setContentText("$caller - Video Call")
            } else {
                notification.setContentText("$caller - Voice Call")
            }
            notification
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.InboxStyle())

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification.build())
        }

        private fun createChannel(context: android.content.Context) {
            val channel = NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH
            ).apply {
                setName("Incoming Messages")
                setDescription("Notifications for new messages and calls")
                setSoundAttributes()
            }.build()

            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}