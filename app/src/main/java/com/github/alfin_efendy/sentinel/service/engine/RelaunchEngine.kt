package com.github.alfin_efendy.sentinel.service.engine

import android.content.Context
import android.util.Log
import com.github.alfin_efendy.sentinel.core.AppEventBus
import com.github.alfin_efendy.sentinel.core.ServiceStateHolder
import com.github.alfin_efendy.sentinel.core.notification.NotificationHelper
import com.github.alfin_efendy.sentinel.domain.model.AppConfig
import com.github.alfin_efendy.sentinel.domain.model.ForegroundEvent
import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Core state machine for app monitoring and relaunch.
 *
 * Lifecycle: start() begins collecting events; stop() cancels everything.
 * Must be used on a single Dispatchers.Default coroutine context (event handling is sequential).
 */
class RelaunchEngine(
    private val context: Context,
    private val config: AppConfig
) {
    companion object {
        private const val TAG = "RelaunchEngine"

        /**
         * Delay before firing the launch intent after a relaunch is triggered.
         * Prevents the system from showing a white screen when the intent is fired
         * while a previous activity is still animating out.
         */
        private const val RELAUNCH_DELAY_MS = 3_000L

        /** Packages whose foreground events should not trigger relaunch logic. */
        val SYSTEM_EXEMPT_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher2",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",       // Samsung
            "com.miui.home",                       // MIUI
            "com.huawei.android.launcher",         // EMUI
            "com.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val debounce = DebounceController(scope)
    private val launcher = AppLauncher(context)

    private var collectionJob: Job? = null
    private var relaunchCount: Int = 0

    /**
     * True only while the target app is confirmed in the foreground.
     * Set to true by AppEnteredForeground(target), false by AppLeftForeground(target).
     * Starts as false so the watchdog can trigger an initial relaunch if app is not open.
     */
    private var targetIsInForeground = false

    fun start() {
        Log.d(TAG, "Engine starting for target: ${config.packageName}")
        ServiceStateHolder.setState(MonitoringState.Monitoring(config.packageName))
        collectionJob = scope.launch {
            AppEventBus.events.collect { event ->
                handleEvent(event)
            }
        }
        startWatchdog()
    }

    /**
     * Periodically checks if the target is actually in the foreground while in Monitoring state.
     * Handles cases where AppLeftForeground was missed, or after grace/relaunching cycles
     * where the app ended up closed but the engine is stuck in Monitoring.
     */
    private fun startWatchdog() {
        scope.launch {
            delay(5_000L) // give initial launch time to open first
            while (true) {
                val state = ServiceStateHolder.currentState
                if (state is MonitoringState.Monitoring && !targetIsInForeground && !debounce.isPending) {
                    Log.d(TAG, "Watchdog: target not in foreground while monitoring — scheduling relaunch")
                    debounce.schedule(config.debounceMs) { triggerRelaunch() }
                }
                delay(5_000L)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Engine stopping")
        debounce.cancel()
        collectionJob?.cancel()
        collectionJob = null
        ServiceStateHolder.setState(MonitoringState.Idle)
    }

    fun pause() {
        debounce.cancel()
        ServiceStateHolder.setState(MonitoringState.Paused(config.packageName))
        Log.d(TAG, "Monitoring paused by user")
    }

    fun resume() {
        ServiceStateHolder.setState(MonitoringState.Monitoring(config.packageName))
        Log.d(TAG, "Monitoring resumed by user")
    }

    private fun handleEvent(event: ForegroundEvent) {
        when (event) {
            is ForegroundEvent.AppEnteredForeground -> onAppEnteredForeground(event.packageName)
            is ForegroundEvent.AppLeftForeground -> onAppLeftForeground(event.packageName)
            is ForegroundEvent.AccessibilityConnected -> {
                val current = ServiceStateHolder.currentState
                if (current is MonitoringState.Idle) {
                    ServiceStateHolder.setState(MonitoringState.Monitoring(config.packageName))
                }
            }
            is ForegroundEvent.AccessibilityDisconnected -> {
                debounce.cancel()
                ServiceStateHolder.setState(MonitoringState.Idle)
                NotificationHelper.showAlert(
                    context,
                    "Sentinel — Accessibility Disconnected",
                    "Re-enable Sentinel in Settings > Accessibility to resume monitoring."
                )
            }
        }
    }

    private fun onAppEnteredForeground(packageName: String) {
        if (packageName == config.packageName) {
            targetIsInForeground = true
            debounce.cancel()

            val current = ServiceStateHolder.currentState
            if (current is MonitoringState.Relaunching || current is MonitoringState.GracePeriod) {
                transitionToGracePeriod()
            } else if (current is MonitoringState.Monitoring) {
                // Already monitoring — no state change needed, watchdog will stay silent
            }
            Log.d(TAG, "Target entered foreground")
        }
    }

    private fun onAppLeftForeground(packageName: String) {
        if (packageName != config.packageName) return

        targetIsInForeground = false

        val current = ServiceStateHolder.currentState
        when (current) {
            is MonitoringState.Monitoring -> {
                Log.d(TAG, "Target left foreground — scheduling relaunch in ${config.debounceMs}ms")
                debounce.schedule(config.debounceMs) { triggerRelaunch() }
            }
            is MonitoringState.GracePeriod -> {
                // App closed again before grace period expired — cancel grace and relaunch
                Log.d(TAG, "Target left foreground during grace period — cancelling grace, scheduling relaunch")
                ServiceStateHolder.setState(MonitoringState.Monitoring(config.packageName))
                debounce.schedule(config.debounceMs) { triggerRelaunch() }
            }
            else -> {
                Log.d(TAG, "Target left foreground but state=$current — watchdog will handle")
            }
        }
    }

    private suspend fun triggerRelaunch() {
        val current = ServiceStateHolder.currentState
        if (current !is MonitoringState.Monitoring) {
            Log.d(TAG, "triggerRelaunch called but state=$current — skipping")
            return
        }

        relaunchCount++
        Log.i(TAG, "Launching target (attempt $relaunchCount): ${config.packageName}")

        ServiceStateHolder.setState(MonitoringState.Relaunching(config.packageName, relaunchCount))

        // Brief pause so the system can finish any ongoing activity transitions,
        // preventing a white/blank screen caused by firing an intent too aggressively.
        delay(RELAUNCH_DELAY_MS)

        val launched = launcher.launch(config.packageName, config.deepLinkUrl)
        if (!launched) {
            // App may be uninstalled
            Log.e(TAG, "Launch failed — app may not be installed: ${config.packageName}")
            NotificationHelper.showAlert(
                context,
                "Sentinel — App Not Found",
                "Could not launch ${config.packageName}. Is it still installed?"
            )
            ServiceStateHolder.setState(MonitoringState.Idle)
            return
        }

        // Fallback: if target never enters foreground within 5s, still exit RELAUNCHING state
        scope.launch {
            delay(5_000L)
            if (ServiceStateHolder.currentState is MonitoringState.Relaunching) {
                transitionToGracePeriod()
            }
        }
    }

    private fun transitionToGracePeriod() {
        val expiresAt = System.currentTimeMillis() + config.gracePeriodMs
        ServiceStateHolder.setState(MonitoringState.GracePeriod(config.packageName, expiresAt))
        Log.d(TAG, "Grace period started (${config.gracePeriodMs}ms)")
        scope.launch {
            delay(config.gracePeriodMs)
            if (ServiceStateHolder.currentState is MonitoringState.GracePeriod) {
                ServiceStateHolder.setState(MonitoringState.Monitoring(config.packageName))
                if (!targetIsInForeground) {
                    Log.d(TAG, "Grace period expired but target not in foreground — scheduling relaunch")
                    debounce.schedule(config.debounceMs) { triggerRelaunch() }
                } else {
                    Log.d(TAG, "Grace period expired — back to MONITORING")
                }
            }
        }
    }

}
