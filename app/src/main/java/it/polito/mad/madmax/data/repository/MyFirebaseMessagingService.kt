package it.polito.mad.madmax.data.repository

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.polito.mad.madmax.MainActivity
import it.polito.mad.madmax.R
import java.util.*

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupChannels(notificationManager)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.fcm_notification_channel_id))
            .setContentTitle(remoteMessage.data["title"])
            .setContentText(remoteMessage.data["message"])
            .setAutoCancel(true)
            .setSound(notificationSoundUri)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher_round)

        //Set notification color to match your app color template
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.color = resources.getColor(R.color.colorPrimaryDark)
        }
        notificationManager.notify(Random().nextInt(3000), notificationBuilder.build())
    }

    private fun sendNotification(messageBody: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            RC_NOTIFICATION,
            Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) },
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.fcm_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            //.setSmallIcon(R.mipmap.ic_launcher_round)
            //.setContentTitle(remoteMessage.data["title"])
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 , notificationBuilder.build())
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupChannels(notificationManager: NotificationManager?) {
        val adminChannelName = "New notification"
        val adminChannelDescription = "Device to device notification"

        val adminChannel = NotificationChannel(
            getString(R.string.fcm_notification_channel_id),
            adminChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = adminChannelDescription
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
        }
        notificationManager?.createNotificationChannel(adminChannel)
    }

    // Companion
    companion object {
        const val TAG = "MM_NOTIFICATIONS"
        const val RC_NOTIFICATION = 0
    }
}
