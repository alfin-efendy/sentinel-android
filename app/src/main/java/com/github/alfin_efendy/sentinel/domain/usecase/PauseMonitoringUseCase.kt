package com.github.alfin_efendy.sentinel.domain.usecase

import android.content.Context
import android.content.Intent
import com.github.alfin_efendy.sentinel.service.SentinelForegroundService

class PauseMonitoringUseCase(private val context: Context) {

    operator fun invoke() {
        val intent = Intent(context, SentinelForegroundService::class.java).apply {
            action = SentinelForegroundService.ACTION_PAUSE_MONITORING
        }
        context.startService(intent)
    }
}
