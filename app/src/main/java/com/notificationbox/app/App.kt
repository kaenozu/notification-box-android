package com.notificationbox.app

import android.app.Application
import com.notificationbox.app.data.NotificationPreferences

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationPreferences.initialize(this)
    }
}
