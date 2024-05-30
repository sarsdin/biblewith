package jm.preversion.biblewith.rtc

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import jm.preversion.biblewith.R
import jm.preversion.biblewith.MainActivity


class MediaProjectionService: Service() {

    private val CHANNEL_ID = "BibleWithScreenShare"
    private val NOTIFICATION_ID = 14

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification: Notification = createNotification()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_IMMUTABLE)
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BibleWith ScreenSharing")
            .setContentText("화면공유중...")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ddam)
            .setOngoing(true)
            .build()
        return notification
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onDestroy()
    }
}