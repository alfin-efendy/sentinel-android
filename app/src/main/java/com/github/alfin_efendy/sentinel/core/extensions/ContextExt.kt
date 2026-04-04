package com.github.alfin_efendy.sentinel.core.extensions

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

fun Context.startForegroundServiceCompat(intent: Intent) {
    ContextCompat.startForegroundService(this, intent)
}
