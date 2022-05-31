package com.romes.chatapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.romes.chatapp.R
import com.romes.chatapp.activities.MainActivity
import com.romes.chatapp.activities.SignInActivity
import com.romes.chatapp.firebase.FirestoreClass
import com.romes.chatapp.utils.Constants

class MyFirebaseInstanceIDService: FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(Constants.FIREBASE_MESSAGE_TAG, "FROM ${message.from}")
        if (message.data.isNotEmpty()) {
            Log.d(Constants.FIREBASE_MESSAGE_TAG, "MESSAGE PAYLOAD ${message.data}")
            val title = message.data[Constants.FCM_KEY_TITLE]!!
            val messageSend = message.data[Constants.FCM_KEY_MESSAGE]!!
            sendNotification(messageSend, title)
        }
        message.notification?.let {
            Log.d(Constants.FIREBASE_MESSAGE_TAG, "MESSAGE NOTIFICATION BODY ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(Constants.FIREBASE_MESSAGE_TAG, "New Token: $token")
        sendRegistrationToServer(token)

    }

    private fun sendRegistrationToServer(token: String?) {
    }

    private fun sendNotification(messageBody: String, title: String) {
        val intent = if (FirestoreClass().getCurrentUserId().isNotEmpty()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, SignInActivity::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = this.resources.getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(
            this, channelId,
        ).setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel Chat App",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notificationBuilder.build())

    }

}