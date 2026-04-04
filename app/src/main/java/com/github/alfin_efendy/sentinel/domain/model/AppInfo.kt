package com.github.alfin_efendy.sentinel.domain.model

import android.graphics.drawable.Drawable

/** Lightweight representation of an installed app shown in the app picker. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)
