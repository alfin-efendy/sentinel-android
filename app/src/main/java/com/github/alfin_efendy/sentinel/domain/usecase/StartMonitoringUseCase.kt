package com.github.alfin_efendy.sentinel.domain.usecase

import android.content.Context
import android.content.Intent
import com.github.alfin_efendy.sentinel.core.extensions.startForegroundServiceCompat
import com.github.alfin_efendy.sentinel.data.repository.AppConfigRepository
import com.github.alfin_efendy.sentinel.domain.model.AppConfig
import com.github.alfin_efendy.sentinel.service.SentinelForegroundService

class StartMonitoringUseCase(
    private val context: Context,
    private val repository: AppConfigRepository,
    private val saveConfig: SaveConfigUseCase
) {

    sealed class Result {
        object Success : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(config: AppConfig): Result {
        val saveResult = saveConfig(config.copy(isEnabled = true))
        if (saveResult is SaveConfigUseCase.Result.Error) {
            return Result.Error(saveResult.message)
        }

        val intent = Intent(context, SentinelForegroundService::class.java).apply {
            action = SentinelForegroundService.ACTION_START_MONITORING
        }
        context.startForegroundServiceCompat(intent)
        return Result.Success
    }
}
