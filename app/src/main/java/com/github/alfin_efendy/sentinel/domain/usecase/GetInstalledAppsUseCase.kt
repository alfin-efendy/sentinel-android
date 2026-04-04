package com.github.alfin_efendy.sentinel.domain.usecase

import com.github.alfin_efendy.sentinel.data.repository.PackageInfoRepository
import com.github.alfin_efendy.sentinel.domain.model.AppInfo

class GetInstalledAppsUseCase(private val repository: PackageInfoRepository) {

    suspend operator fun invoke(): List<AppInfo> = repository.getInstalledApps()
}
