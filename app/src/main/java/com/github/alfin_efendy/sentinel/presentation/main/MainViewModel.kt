package com.github.alfin_efendy.sentinel.presentation.main

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.alfin_efendy.sentinel.core.ServiceStateHolder
import com.github.alfin_efendy.sentinel.core.extensions.startForegroundServiceCompat
import com.github.alfin_efendy.sentinel.data.datastore.AppConfigDataStore
import com.github.alfin_efendy.sentinel.data.repository.AppConfigRepository
import com.github.alfin_efendy.sentinel.data.repository.PackageInfoRepository
import com.github.alfin_efendy.sentinel.domain.model.AppConfig
import com.github.alfin_efendy.sentinel.domain.model.AppInfo
import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import com.github.alfin_efendy.sentinel.domain.usecase.GetInstalledAppsUseCase
import com.github.alfin_efendy.sentinel.domain.usecase.SaveConfigUseCase
import com.github.alfin_efendy.sentinel.domain.usecase.StartMonitoringUseCase
import com.github.alfin_efendy.sentinel.service.SentinelForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = application.applicationContext
    private val dataStore = AppConfigDataStore(ctx)
    private val configRepo = AppConfigRepository(dataStore)
    private val packageRepo = PackageInfoRepository(ctx)
    private val saveConfig = SaveConfigUseCase(configRepo)
    private val getApps = GetInstalledAppsUseCase(packageRepo)
    private val startMonitoring = StartMonitoringUseCase(ctx, configRepo, saveConfig)

    val monitoringState: StateFlow<MonitoringState> = ServiceStateHolder.state

    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val installedApps: StateFlow<List<AppInfo>> = combine(_installedApps, _searchQuery) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    init {
        viewModelScope.launch {
            configRepo.configFlow.collect { _config.value = it }
        }
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = getApps()
            _isLoadingApps.value = false
        }
    }

    fun selectApp(app: AppInfo) {
        _config.value = _config.value.copy(packageName = app.packageName)
    }

    fun setDeepLink(url: String) {
        _config.value = _config.value.copy(deepLinkUrl = url.takeIf { it.isNotBlank() })
    }

    fun startMonitoring() {
        viewModelScope.launch {
            val result = startMonitoring(_config.value)
            if (result is StartMonitoringUseCase.Result.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun stopMonitoring() {
        val intent = Intent(ctx, SentinelForegroundService::class.java).apply {
            action = SentinelForegroundService.ACTION_STOP_MONITORING
        }
        ctx.startService(intent)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
