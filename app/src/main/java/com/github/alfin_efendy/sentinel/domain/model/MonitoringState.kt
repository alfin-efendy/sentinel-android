package com.github.alfin_efendy.sentinel.domain.model

/**
 * Runtime state machine for the monitoring lifecycle.
 *
 *  IDLE → MONITORING → RELAUNCHING → GRACE_PERIOD → MONITORING
 *                    ↘ PAUSED (user intent) → MONITORING
 *  ANY → IDLE (stop / a11y disconnected)
 */
sealed class MonitoringState {

    /** Service running but no valid target configured, or monitoring explicitly stopped. */
    object Idle : MonitoringState()

    /** Actively watching target app; will trigger relaunch if it leaves foreground. */
    data class Monitoring(val targetPackage: String) : MonitoringState()

    /** Relaunch intent has been fired; waiting for target to appear in foreground. */
    data class Relaunching(
        val targetPackage: String,
        val attemptCount: Int
    ) : MonitoringState()

    /**
     * Post-relaunch suppression window.
     * All relaunch triggers are ignored until [expiresAt].
     */
    data class GracePeriod(
        val targetPackage: String,
        val expiresAt: Long
    ) : MonitoringState()

    /** User explicitly paused monitoring via the overlay long-press or pause button. */
    data class Paused(val targetPackage: String) : MonitoringState()

    val isActive: Boolean
        get() = this !is Idle && this !is Paused

    val targetPackageOrNull: String?
        get() = when (this) {
            is Monitoring -> targetPackage
            is Relaunching -> targetPackage
            is GracePeriod -> targetPackage
            is Paused -> targetPackage
            else -> null
        }
}
