package com.example.pleos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.pleos.R

/**
 * Keeps the app process running while another app covers it. On the Pleos emulator Android froze
 * the cached process ("freezing com.example.pleos") as soon as the navigation took the window, and
 * a frozen process misses the drive and the charge it should record. The monitor itself still runs
 * in the application scope; this service only holds the process in the foreground state.
 *
 * Type specialUse, as tested on the emulator (research notes 11.2).
 */
class VehicleRecordingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.recording_channel), NotificationManager.IMPORTANCE_MIN),
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.recording_title))
            .setContentText(getString(R.string.recording_text))
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        return START_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "vehicle_recording"
        private const val NOTIFICATION_ID = 1

        /** Call while the app is in the foreground; Android does not allow starting it from the background. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, VehicleRecordingService::class.java))
        }
    }
}
