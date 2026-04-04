package com.github.alfin_efendy.sentinel.presentation.onboarding

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.alfin_efendy.sentinel.presentation.common.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PermissionStepState(
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val buttonLabel: String
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val _steps = MutableStateFlow(buildSteps())
    val steps: StateFlow<List<PermissionStepState>> = _steps.asStateFlow()

    val allGranted: Boolean
        get() = _steps.value.all { it.isGranted }

    fun refresh() {
        _steps.value = buildSteps()
    }

    private fun buildSteps(): List<PermissionStepState> {
        val ctx = getApplication<Application>()
        return buildList {
            add(
                PermissionStepState(
                    title = "Accessibility Service",
                    description = "Required to detect when the target app is in the foreground. Find 'Sentinel' in the list and enable it.",
                    isGranted = PermissionHelper.isAccessibilityEnabled(ctx),
                    buttonLabel = "Open Accessibility Settings"
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    PermissionStepState(
                        title = "Notification Permission",
                        description = "Required to show the persistent monitoring notification.",
                        isGranted = PermissionHelper.isNotificationPermissionGranted(ctx),
                        buttonLabel = "Grant Notification Permission"
                    )
                )
            }
            add(
                PermissionStepState(
                    title = "Battery Optimization (Recommended)",
                    description = "Exclude Sentinel from battery optimization to prevent the service from being killed on some devices.",
                    isGranted = PermissionHelper.isIgnoringBatteryOptimizations(ctx),
                    buttonLabel = "Disable Battery Optimization"
                )
            )
        }
    }
}
