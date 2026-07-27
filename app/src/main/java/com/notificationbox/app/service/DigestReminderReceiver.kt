package com.notificationbox.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.notificationbox.app.App
import com.notificationbox.app.MainActivity
import com.notificationbox.app.R
import com.notificationbox.app.model.NotificationDecision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DigestReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as App
                val count = app.container.notificationRepository.observeNotifications()
                    .first()
                    .count { it.isActive && it.category == NotificationDecision.HoldForDigest }
                if (count == 0) return@launch

                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.digest_reminder_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
                val openApp = PendingIntent.getActivity(
                    context,
                    OPEN_APP_REQUEST_CODE,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                manager.notify(
                    NOTIFICATION_ID,
                    NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_monochrome)
                        .setContentTitle(context.getString(R.string.digest_reminder_title))
                        .setContentText(context.getString(R.string.digest_reminder_body, count))
                        .setContentIntent(openApp)
                        .setAutoCancel(true)
                        .build()
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "digest_reminder"
        private const val NOTIFICATION_ID = 1902
        private const val OPEN_APP_REQUEST_CODE = 1903
    }
}
