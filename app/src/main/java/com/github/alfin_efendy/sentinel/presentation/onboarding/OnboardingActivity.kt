package com.github.alfin_efendy.sentinel.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.alfin_efendy.sentinel.presentation.common.PermissionHelper
import com.github.alfin_efendy.sentinel.presentation.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val dp16 = (16 * resources.displayMetrics.density).toInt()
            setPadding(dp16, dp16, dp16, dp16)
        }
        root.addView(container)
        setContentView(root)

        val dp8 = (8 * resources.displayMetrics.density).toInt()

        // Title
        val titleView = TextView(this).apply {
            text = "Setup Sentinel"
            textSize = 24f
            val dp16 = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, dp16)
        }
        container.addView(titleView)

        val subtitleView = TextView(this).apply {
            text = "Grant the following permissions to enable app monitoring."
            textSize = 14f
            val dp16 = (16 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, dp16)
        }
        container.addView(subtitleView)

        // Steps container (dynamically populated)
        val stepsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(stepsContainer)

        // Continue button
        val continueBtn = Button(this).apply {
            text = "Continue to App"
            isEnabled = false
            val dp16 = (16 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp16, 0, 0) }
            layoutParams = params
            setOnClickListener { navigateToMain() }
        }
        container.addView(continueBtn)

        lifecycleScope.launch {
            viewModel.steps.collectLatest { steps ->
                stepsContainer.removeAllViews()
                steps.forEachIndexed { index, step ->
                    addStepView(stepsContainer, index + 1, step, dp8)
                }
                val required = steps.dropLast(1) // last step (battery) is optional
                continueBtn.isEnabled = required.all { it.isGranted }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        if (PermissionHelper.areRequiredPermissionsGranted(this)) {
            navigateToMain()
        }
    }

    private fun addStepView(
        container: LinearLayout,
        stepNumber: Int,
        step: PermissionStepState,
        dp8: Int
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp8 * 2) }
            layoutParams = params
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val statusIndicator = TextView(this).apply {
            text = if (step.isGranted) "✓" else "$stepNumber"
            textSize = 16f
            setTextColor(
                if (step.isGranted) android.graphics.Color.parseColor("#4CAF50")
                else android.graphics.Color.parseColor("#F44336")
            )
            val dp8px = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, dp8px, 0)
        }
        header.addView(statusIndicator)

        val titleView = TextView(this).apply {
            text = step.title
            textSize = 16f
        }
        header.addView(titleView)
        row.addView(header)

        val descView = TextView(this).apply {
            text = step.description
            textSize = 13f
            setTextColor(android.graphics.Color.GRAY)
            val dp8px = (8 * resources.displayMetrics.density).toInt()
            setPadding(dp8px * 3, dp8px / 2, 0, dp8px / 2)
        }
        row.addView(descView)

        if (!step.isGranted) {
            val btn = Button(this).apply {
                text = step.buttonLabel
                val btnParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    val dp8px = (8 * resources.displayMetrics.density).toInt()
                    setMargins(dp8px * 3, dp8px / 2, 0, 0)
                }
                layoutParams = btnParams
                setOnClickListener { handleStepGrant(stepNumber - 1) }
            }
            row.addView(btn)
        }

        container.addView(row)
    }

    /**
     * Resolves the action for a step by looking up its title rather than using a fixed index,
     * because the notification step is conditionally absent on API < 33 which would shift
     * all subsequent indices and cause the wrong action to fire.
     */
    private fun handleStepGrant(stepIndex: Int) {
        val step = viewModel.steps.value.getOrNull(stepIndex) ?: return
        when {
            step.title.contains("Accessibility") ->
                startActivity(PermissionHelper.buildAccessibilitySettingsIntent())
            step.title.contains("Notification") -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            step.title.contains("Battery") ->
                startActivity(PermissionHelper.buildBatteryOptimizationIntent(this))
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
