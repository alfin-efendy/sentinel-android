package com.github.alfin_efendy.sentinel.presentation.main

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.alfin_efendy.sentinel.domain.model.AppInfo
import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import com.github.alfin_efendy.sentinel.presentation.common.PermissionHelper
import com.github.alfin_efendy.sentinel.presentation.onboarding.OnboardingActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // View references
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var statusSubtext: TextView
    private lateinit var selectBoxIcon: ImageView
    private lateinit var selectBoxLabel: TextView
    private lateinit var selectBoxPkg: TextView
    private lateinit var deepLinkInput: EditText
    private lateinit var toggleButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PermissionHelper.areRequiredPermissionsGranted(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        buildUi()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionHelper.areRequiredPermissionsGranted(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }

    // ── UI construction ──────────────────────────────────────────────────────────

    private fun buildUi() {
        window.decorView.setBackgroundColor(Color.parseColor("#F8FAFC"))

        val scroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(buildHeader())
        root.addView(spacer(dp(20)))
        root.addView(buildStatusCard())
        root.addView(spacer(dp(16)))
        root.addView(buildSectionLabel("Target Application"))
        root.addView(spacer(dp(8)))
        root.addView(buildAppSelectBox())
        root.addView(spacer(dp(16)))
        root.addView(buildSectionLabel("Deep Link (optional)"))
        root.addView(spacer(dp(8)))
        root.addView(buildDeepLinkInput())
        root.addView(spacer(dp(28)))
        root.addView(buildToggleButton())
    }

    private fun buildHeader(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        addView(TextView(this@MainActivity).apply {
            text = "Sentinel"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1E293B"))
        })
        addView(TextView(this@MainActivity).apply {
            text = "App Monitor & Auto-Relaunch"
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
    }

    private fun buildStatusCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = card(Color.WHITE, dp(16))
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        statusDot = View(this).apply {
            background = circle(Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                setMargins(0, 0, dp(10), 0)
            }
        }
        statusText = TextView(this).apply {
            text = "Idle"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1E293B"))
        }
        topRow.addView(statusDot)
        topRow.addView(statusText)
        card.addView(topRow)

        statusSubtext = TextView(this).apply {
            text = "No target selected"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        }
        card.addView(statusSubtext)

        return card
    }

    private fun buildAppSelectBox(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = card(Color.WHITE, dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener { openAppPicker() }
        }

        // App icon container
        val iconContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                setMargins(0, 0, dp(14), 0)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F5F9"))
                cornerRadius = dp(10).toFloat()
            }
        }
        selectBoxIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(28), dp(28)).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        iconContainer.addView(selectBoxIcon)
        box.addView(iconContainer)

        // Text column
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        selectBoxLabel = TextView(this).apply {
            text = "Select an app..."
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#94A3B8"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        selectBoxPkg = TextView(this).apply {
            text = ""
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            visibility = View.GONE
        }
        textCol.addView(selectBoxLabel)
        textCol.addView(selectBoxPkg)
        box.addView(textCol)

        // Chevron
        box.addView(TextView(this).apply {
            text = "›"
            textSize = 22f
            setTextColor(Color.parseColor("#CBD5E1"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(8), 0, 0, 0) }
        })

        return box
    }

    private fun buildDeepLinkInput(): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = card(Color.WHITE, dp(14))
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }
        deepLinkInput = EditText(this).apply {
            hint = "roblox://placeId=<id>  or  https://..."
            textSize = 13f
            setTextColor(Color.parseColor("#1E293B"))
            setHintTextColor(Color.parseColor("#CBD5E1"))
            background = null
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
            setPadding(0, dp(10), 0, dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = viewModel.setDeepLink(s.toString())
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        container.addView(deepLinkInput)
        return container
    }

    private fun buildToggleButton(): TextView {
        toggleButton = TextView(this).apply {
            text = "Start Monitoring"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = card(Color.parseColor("#6366F1"), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (viewModel.monitoringState.value.isActive) viewModel.stopMonitoring()
                else viewModel.startMonitoring()
            }
        }
        return toggleButton
    }

    // ── Observe ViewModel ────────────────────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.monitoringState.collectLatest { updateToggleUi(it) }
        }
        lifecycleScope.launch {
            viewModel.config.collectLatest { config ->
                // Update select box
                val appInfo = viewModel.installedApps.value
                    .firstOrNull { it.packageName == config.packageName }
                updateSelectBox(appInfo, config.packageName)

                // Update status subtext
                statusSubtext.text = config.packageName.ifBlank { "No target selected" }

                // Sync deep link input without triggering TextWatcher
                if (deepLinkInput.text.toString() != (config.deepLinkUrl ?: "")) {
                    deepLinkInput.setText(config.deepLinkUrl ?: "")
                }
            }
        }
        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { msg ->
                msg?.let {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Error")
                        .setMessage(it)
                        .setPositiveButton("OK") { _, _ -> viewModel.clearError() }
                        .show()
                }
            }
        }
    }

    private fun updateSelectBox(app: AppInfo?, packageName: String) {
        if (app != null) {
            selectBoxIcon.setImageDrawable(app.icon)
            selectBoxLabel.text = app.label
            selectBoxLabel.setTextColor(Color.parseColor("#1E293B"))
            selectBoxLabel.typeface = Typeface.DEFAULT_BOLD
            selectBoxPkg.text = app.packageName
            selectBoxPkg.visibility = View.VISIBLE
        } else if (packageName.isNotBlank()) {
            selectBoxIcon.setImageDrawable(null)
            selectBoxLabel.text = packageName
            selectBoxLabel.setTextColor(Color.parseColor("#64748B"))
            selectBoxPkg.visibility = View.GONE
        } else {
            selectBoxIcon.setImageDrawable(null)
            selectBoxLabel.text = "Select an app..."
            selectBoxLabel.setTextColor(Color.parseColor("#94A3B8"))
            selectBoxLabel.typeface = Typeface.DEFAULT
            selectBoxPkg.visibility = View.GONE
        }
    }

    private fun updateToggleUi(state: MonitoringState) {
        val (statusLabel, dotColor, btnText, btnColor) = when (state) {
            is MonitoringState.Idle ->
                Quad("Idle", "#94A3B8", "Start Monitoring", "#6366F1")
            is MonitoringState.Monitoring ->
                Quad("Monitoring", "#10B981", "Stop Monitoring", "#EF4444")
            is MonitoringState.Relaunching ->
                Quad("Relaunching (${state.attemptCount}×)", "#F59E0B", "Stop Monitoring", "#EF4444")
            is MonitoringState.GracePeriod ->
                Quad("Grace Period", "#F59E0B", "Stop Monitoring", "#EF4444")
            is MonitoringState.Paused ->
                Quad("Paused", "#94A3B8", "Resume Monitoring", "#6366F1")
        }
        statusText.text = statusLabel
        (statusDot.background as? GradientDrawable)?.setColor(Color.parseColor(dotColor))
        toggleButton.text = btnText
        toggleButton.background = card(Color.parseColor(btnColor), dp(14))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun openAppPicker() {
        if (supportFragmentManager.findFragmentByTag(AppPickerBottomSheet.TAG) == null) {
            AppPickerBottomSheet().show(supportFragmentManager, AppPickerBottomSheet.TAG)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun spacer(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }

    private fun buildSectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.parseColor("#64748B"))
        letterSpacing = 0.08f
    }

    private fun card(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private data class Quad(
        val status: String,
        val dotColor: String,
        val btnText: String,
        val btnColor: String
    )
}
