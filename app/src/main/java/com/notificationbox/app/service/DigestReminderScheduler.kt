package com.notificationbox.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class DigestReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun scheduleInOneHour() {
        val intent = Intent(appContext, DigestReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + ONE_HOUR_MILLIS,
            pendingIntent
        )
    }

    companion object {
        private const val REQUEST_CODE = 1901
        private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    }
}
