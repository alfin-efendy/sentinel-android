package com.github.alfin_efendy.sentinel

import android.app.Application
import com.github.alfin_efendy.sentinel.core.notification.NotificationHelper

class SentinelApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
