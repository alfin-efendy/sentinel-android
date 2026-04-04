package com.github.alfin_efendy.sentinel.data.repository

import com.github.alfin_efendy.sentinel.data.datastore.AppConfigDataStore
import com.github.alfin_efendy.sentinel.domain.model.AppConfig
import kotlinx.coroutines.flow.Flow

class AppConfigRepository(private val dataStore: AppConfigDataStore) {

    val configFlow: Flow<AppConfig> = dataStore.configFlow

    suspend fun save(config: AppConfig) = dataStore.save(config)

    suspend fun readOnce(): AppConfig = dataStore.readOnce()
}
