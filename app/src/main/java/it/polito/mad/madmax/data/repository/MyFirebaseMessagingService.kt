package it.polito.mad.madmax.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.polito.mad.madmax.MainActivity
import it.polito.mad.madmax.R
import it.polito.mad.madmax.displayMessage
import org.json.JSONObject
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Create intent
        val pendingIntent = PendingIntent.getActivity(
            this,
            RC_NOTIFICATION,
            Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) },
            PendingIntent.FLAG_ONE_SHOT
        )

        // Retrieve notification manager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel (
                getString(R.string.fcm_interest_channel_id),
                getString(R.string.fcm_interest_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.fcm_interest_channel_description)
            })
        }

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.fcm_interest_channel_id))
            .setContentTitle(remoteMessage.data["title"])
            .setContentText(remoteMessage.data["message"])
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setColor(resources.getColor(R.color.colorPrimary))

        notificationManager.notify(Random().nextInt(3000), notificationBuilder.build())
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.d(TAG, "New token")
    }

    // Companion
    companion object {
        const val TAG = "MM_NOTIFICATIONS"
        const val RC_NOTIFICATION = 0

        fun sendNotification(context: Context, notification: JSONObject) {
            Volley.newRequestQueue(context).add(object: JsonObjectRequest(
                context.getString(R.string.fcm_api_url),
                notification,
                Response.Listener { response ->
                    Log.d(TAG, "onResponse: $response")
                },
                Response.ErrorListener {
                    displayMessage(context, "Error request ${it.message.toString()}")
                    Log.d(TAG, "onErrorResponse: Didn't work")
                }
            ) {
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["Authorization"] = "key=" + context.getString(R.string.fcm_server_key)
                    params["Content-Type"] = context.getString(R.string.fcm_content_type)
                    return params
                }
            })
        }

        fun createNotification(topicId: String, title: String, message: String): JSONObject {
            return JSONObject().apply {
                put("to", "/topics/$topicId")
                put("data", JSONObject().apply {
                    put("title", title)
                    put("message", message)
                })
            }
        }
    }
}
