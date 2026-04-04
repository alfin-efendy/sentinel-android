package com.github.alfin_efendy.sentinel.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.github.alfin_efendy.sentinel.core.ServiceStateHolder
import com.github.alfin_efendy.sentinel.core.notification.NotificationHelper
import com.github.alfin_efendy.sentinel.data.datastore.AppConfigDataStore
import com.github.alfin_efendy.sentinel.domain.model.AppConfig
import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import com.github.alfin_efendy.sentinel.service.engine.AppLauncher
import com.github.alfin_efendy.sentinel.service.engine.RelaunchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Persistent foreground service that owns the RelaunchEngine.
 * Returns START_STICKY so the system restarts it after being killed.
 */
class SentinelForegroundService : Service() {

    companion object {
        private const val TAG = "SentinelFgService"
        const val ACTION_START_MONITORING = "com.github.alfin_efendy.sentinel.START"
        const val ACTION_PAUSE_MONITORING = "com.github.alfin_efendy.sentinel.PAUSE"
        const val ACTION_RESUME_MONITORING = "com.github.alfin_efendy.sentinel.RESUME"
        const val ACTION_STOP_MONITORING = "com.github.alfin_efendy.sentinel.STOP"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engine: RelaunchEngine? = null
    private var notificationJob: Job? = null
    private lateinit var dataStore: AppConfigDataStore

    override fun onCreate() {
        super.onCreate()
        dataStore = AppConfigDataStore(applicationContext)
        NotificationHelper.createChannels(this)
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground() quickly to avoid ANR on Android 8+
        val idleNotification = NotificationHelper.buildIdleNotification(this)
        startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND, idleNotification)

        when (intent?.action) {
            ACTION_START_MONITORING -> {
                serviceScope.launch {
                    val config = dataStore.configFlow.first()
                    startMonitoring(config)
                }
            }
            ACTION_PAUSE_MONITORING -> engine?.pause()
            ACTION_RESUME_MONITORING -> engine?.resume()
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // Service restarted by system (START_STICKY) — restore from DataStore
                serviceScope.launch {
                    val config = dataStore.configFlow.first()
                    if (config.isEnabled && config.isValid) {
                        Log.d(TAG, "Service restarted by system — resuming monitoring")
                        startMonitoring(config)
                    }
                }
            }
        }
        return START_STICKY
    }

    private suspend fun startMonitoring(config: AppConfig) {
        if (!config.isValid) {
            Log.w(TAG, "Cannot start monitoring — no valid config")
            return
        }

        // Stop any existing engine before creating a new one
        engine?.stop()
        engine = RelaunchEngine(applicationContext, config)
        engine!!.start()

        // Launch the target app immediately so the user doesn't have to open it manually
        AppLauncher(applicationContext).launch(config.packageName, config.deepLinkUrl)
        Log.d(TAG, "Initial launch fired for: ${config.packageName}")

        // Observe state changes and keep notification up-to-date with actions
        notificationJob?.cancel()
        notificationJob = serviceScope.launch {
            ServiceStateHolder.state.collect { state ->
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val notification = NotificationHelper.buildMonitoringNotification(
                    this@SentinelForegroundService, config.packageName, state
                )
                nm.notify(NotificationHelper.NOTIFICATION_ID_FOREGROUND, notification)
            }
        }

        Log.i(TAG, "Monitoring started for: ${config.packageName}")
    }

    private fun stopMonitoring() {
        notificationJob?.cancel()
        notificationJob = null
        engine?.stop()
        engine = null
        ServiceStateHolder.setState(MonitoringState.Idle)
        Log.i(TAG, "Monitoring stopped")
    }

    /**
     * Called when the user swipes the app away from the Recents screen.
     * android:stopWithTask="false" in the manifest prevents the service from being
     * killed immediately, but some OEMs still kill it. AlarmManager schedules a restart
     * as a safety net so monitoring is never permanently interrupted.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (engine != null) {
            Log.d(TAG, "Task removed while monitoring is active — scheduling restart")
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        val restartIntent = Intent(applicationContext, SentinelForegroundService::class.java).apply {
            action = ACTION_START_MONITORING
        }
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            0,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1_000L,
            pendingIntent
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
