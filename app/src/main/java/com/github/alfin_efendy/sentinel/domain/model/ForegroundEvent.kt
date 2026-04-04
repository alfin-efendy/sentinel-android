package com.github.alfin_efendy.sentinel.domain.model

/**
 * Events emitted by SentinelAccessibilityService into AppEventBus.
 * "Left foreground" is inferred: when a new package enters foreground the previous one left.
 */
sealed class ForegroundEvent {

    data class AppEnteredForeground(
        val packageName: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : ForegroundEvent()

    data class AppLeftForeground(
        val packageName: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : ForegroundEvent()

    data object AccessibilityConnected : ForegroundEvent()

    data object AccessibilityDisconnected : ForegroundEvent()
}
