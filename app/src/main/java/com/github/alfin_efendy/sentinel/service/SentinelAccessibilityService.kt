package com.github.alfin_efendy.sentinel.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.github.alfin_efendy.sentinel.core.AppEventBus
import com.github.alfin_efendy.sentinel.domain.model.ForegroundEvent
import com.github.alfin_efendy.sentinel.service.engine.RelaunchEngine

/**
 * System-managed service that monitors foreground app changes via AccessibilityService.
 *
 * Foreground inference:
 *   TYPE_WINDOW_STATE_CHANGED only fires on entry. We track [lastForegroundPackage] and
 *   infer "left foreground" when a different (non-exempt) package enters foreground.
 *
 * Thread safety:
 *   onAccessibilityEvent() is called on the accessibility thread. We use tryEmit() (non-blocking)
 *   so this thread is never suspended or blocked.
 */
class SentinelAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SentinelA11yService"
    }

    private var lastForegroundPackage: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Ensure the service is configured for window state events
        serviceInfo = serviceInfo?.also { info ->
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 100L
            info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        AppEventBus.tryEmit(ForegroundEvent.AccessibilityConnected)
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val newPackage = event.packageName?.toString() ?: return
        if (newPackage.isBlank()) return

        // Skip input method windows — they overlay the foreground app without replacing it
        val className = event.className?.toString() ?: ""
        if (className.startsWith("android.inputmethodservice")) return

        if (newPackage == lastForegroundPackage) return

        val previous = lastForegroundPackage
        // Always update so we don't emit the same left-foreground event twice
        lastForegroundPackage = newPackage

        val isNewExempt = newPackage in RelaunchEngine.SYSTEM_EXEMPT_PACKAGES
        val isPrevExempt = previous.isBlank() || previous in RelaunchEngine.SYSTEM_EXEMPT_PACKAGES

        // If the previous window was a real app (not launcher/system), it has now left foreground.
        // This fires even when the user presses Home (launcher is exempt but target still "left").
        if (!isPrevExempt) {
            AppEventBus.tryEmit(ForegroundEvent.AppLeftForeground(previous))
        }

        // Only report entry for real (non-exempt) apps
        if (!isNewExempt) {
            AppEventBus.tryEmit(ForegroundEvent.AppEnteredForeground(newPackage))
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AppEventBus.tryEmit(ForegroundEvent.AccessibilityDisconnected)
        Log.w(TAG, "Accessibility service unbound")
        return super.onUnbind(intent)
    }
}
