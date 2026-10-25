package com.veilweather.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MessagingPushService {

    companion object {
        private const val CHANNEL_ID = "push_notifications"
        private const val NOTIFICATION_ID = 2

        fun showIncomingMessageNotification(context: Context, sender: String, message: String) {
            createChannel(context)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("New Message")
                .setContentText("From: $sender")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        fun showCallNotification(context: Context, caller: String, isVideo: Boolean) {
            createChannel(context)
            
            val contentText = if (isVideo) "$caller - Video Call" else "$caller - Voice Call"
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Incoming Call")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
        }

        private fun createChannel(context: Context) {
            val name = "Incoming Messages"
            val descriptionText = "Notifications for new messages and calls"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}