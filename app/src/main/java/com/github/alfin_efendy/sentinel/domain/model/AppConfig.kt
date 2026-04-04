package com.github.alfin_efendy.sentinel.domain.model

/**
 * Configuration for the monitored target app.
 * Persisted in DataStore between sessions.
 */
data class AppConfig(
    val packageName: String = "",
    val deepLinkUrl: String? = null,
    val isEnabled: Boolean = false,
    val debounceMs: Long = 2500L,
    val gracePeriodMs: Long = 8000L,
    val maxRelaunchesPerWindow: Int = 3,
    val windowDurationMs: Long = 60_000L
) {
    val isValid: Boolean get() = packageName.isNotBlank()
}
