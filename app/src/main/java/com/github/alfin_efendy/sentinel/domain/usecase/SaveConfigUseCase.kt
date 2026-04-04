package com.github.alfin_efendy.sentinel.domain.usecase

import com.github.alfin_efendy.sentinel.data.repository.AppConfigRepository
import com.github.alfin_efendy.sentinel.domain.model.AppConfig

class SaveConfigUseCase(private val repository: AppConfigRepository) {

    sealed class Result {
        object Success : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(config: AppConfig): Result {
        if (config.packageName.isBlank()) {
            return Result.Error("Package name must not be empty")
        }
        if (config.deepLinkUrl != null) {
            val uri = android.net.Uri.parse(config.deepLinkUrl)
            if (uri.scheme == null) {
                return Result.Error("Deep link URL has no scheme (e.g. https:// or roblox://)")
            }
        }
        repository.save(config)
        return Result.Success
    }
}
