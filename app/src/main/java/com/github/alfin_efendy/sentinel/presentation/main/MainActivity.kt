package com.github.alfin_efendy.sentinel.presentation.main

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.github.alfin_efendy.sentinel.domain.model.AppInfo
import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import com.github.alfin_efendy.sentinel.presentation.common.PermissionHelper
import com.github.alfin_efendy.sentinel.presentation.onboarding.OnboardingActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val focused = currentFocus
            if (focused is EditText) {
                val rect = Rect()
                focused.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    focused.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE)
                            as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(focused.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    // ── UI construction ──────────────────────────────────────────────────────────

    private fun buildUi() {
        val cs = colorScheme()

        // Edge-to-edge + status bar icon tint
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode()

        window.decorView.setBackgroundColor(Color.parseColor(cs.background))

        val scroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(root)
        setContentView(scroll)

        // Measure status bar height and apply as top padding once the view is laid out
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(dp(20), statusBarHeight + dp(24), dp(20), dp(32))
            insets
        }

        root.addView(buildHeader(cs))
        root.addView(spacer(dp(20)))
        root.addView(buildStatusCard(cs))
        root.addView(spacer(dp(16)))
        root.addView(buildSectionLabel("Target Application", cs))
        root.addView(spacer(dp(8)))
        root.addView(buildAppSelectBox(cs))
        root.addView(spacer(dp(16)))
        root.addView(buildSectionLabel("Deep Link (optional)", cs))
        root.addView(spacer(dp(8)))
        root.addView(buildDeepLinkInput(cs))
        root.addView(spacer(dp(28)))
        root.addView(buildToggleButton(cs))
    }

    private fun buildHeader(cs: ColorScheme): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        addView(TextView(this@MainActivity).apply {
            text = "Sentinel"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(cs.textPrimary))
        })
        addView(TextView(this@MainActivity).apply {
            text = "App Monitor & Auto-Relaunch"
            textSize = 13f
            setTextColor(Color.parseColor(cs.textHint))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
    }

    private fun buildStatusCard(cs: ColorScheme): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = card(Color.parseColor(cs.surface), dp(16))
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
            setTextColor(Color.parseColor(cs.textPrimary))
        }
        topRow.addView(statusDot)
        topRow.addView(statusText)
        card.addView(topRow)

        statusSubtext = TextView(this).apply {
            text = "No target selected"
            textSize = 12f
            setTextColor(Color.parseColor(cs.textHint))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        }
        card.addView(statusSubtext)

        return card
    }

    private fun buildAppSelectBox(cs: ColorScheme): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = card(Color.parseColor(cs.surface), dp(14))
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
                setColor(Color.parseColor(cs.inputBg))
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
            setTextColor(Color.parseColor(cs.textHint))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        selectBoxPkg = TextView(this).apply {
            text = ""
            textSize = 11f
            setTextColor(Color.parseColor(cs.textHint))
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
            setTextColor(Color.parseColor(cs.border))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(8), 0, 0, 0) }
        })

        return box
    }

    private fun buildDeepLinkInput(cs: ColorScheme): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = card(Color.parseColor(cs.surface), dp(14))
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }
        deepLinkInput = EditText(this).apply {
            hint = "roblox://placeId=<id>  or  https://..."
            textSize = 13f
            setTextColor(Color.parseColor(cs.textPrimary))
            setHintTextColor(Color.parseColor(cs.textHint))
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

    private fun buildToggleButton(cs: ColorScheme): TextView {
        toggleButton = TextView(this).apply {
            text = "Start Monitoring"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = card(Color.parseColor(cs.primary), dp(14))
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
        // Fix #1: combine config + installedApps so icon/label resolve on cold start
        lifecycleScope.launch {
            combine(viewModel.config, viewModel.installedApps) { config, apps ->
                Pair(config, apps.firstOrNull { it.packageName == config.packageName })
            }.collectLatest { (config, appInfo) ->
                updateSelectBox(appInfo, config.packageName)
                statusSubtext.text = config.packageName.ifBlank { "No target selected" }
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
        val cs = colorScheme()
        if (app != null) {
            selectBoxIcon.setImageDrawable(app.icon)
            selectBoxLabel.text = app.label
            selectBoxLabel.setTextColor(Color.parseColor(cs.textPrimary))
            selectBoxLabel.typeface = Typeface.DEFAULT_BOLD
            selectBoxPkg.text = app.packageName
            selectBoxPkg.visibility = View.VISIBLE
        } else if (packageName.isNotBlank()) {
            selectBoxIcon.setImageDrawable(null)
            selectBoxLabel.text = packageName
            selectBoxLabel.setTextColor(Color.parseColor(cs.textSecondary))
            selectBoxPkg.visibility = View.GONE
        } else {
            selectBoxIcon.setImageDrawable(null)
            selectBoxLabel.text = "Select an app..."
            selectBoxLabel.setTextColor(Color.parseColor(cs.textHint))
            selectBoxLabel.typeface = Typeface.DEFAULT
            selectBoxPkg.visibility = View.GONE
        }
    }

    private fun updateToggleUi(state: MonitoringState) {
        val (statusLabel, dotColor, btnText, btnColor) = when (state) {
            is MonitoringState.Idle ->
                Quad("Idle", "#94A3B8", "Start Monitoring", colorScheme().primary)
            is MonitoringState.Monitoring ->
                Quad("Monitoring", "#10B981", "Stop Monitoring", "#EF4444")
            is MonitoringState.Relaunching ->
                Quad("Relaunching (${state.attemptCount}×)", "#F59E0B", "Stop Monitoring", "#EF4444")
            is MonitoringState.GracePeriod ->
                Quad("Grace Period", "#F59E0B", "Stop Monitoring", "#EF4444")
            is MonitoringState.Paused ->
                Quad("Paused", "#94A3B8", "Resume Monitoring", colorScheme().primary)
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

    private fun buildSectionLabel(text: String, cs: ColorScheme) = TextView(this).apply {
        this.text = text
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.parseColor(cs.textSecondary))
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

    private fun isDarkMode(): Boolean {
        val flags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return flags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun colorScheme(): ColorScheme = if (isDarkMode()) ColorScheme(
        background    = "#0F172A",
        surface       = "#1E293B",
        primary       = "#818CF8",
        textPrimary   = "#F1F5F9",
        textSecondary = "#94A3B8",
        textHint      = "#475569",
        border        = "#334155",
        inputBg       = "#1E293B",
        selectedBg    = "#1E1B4B",
    ) else ColorScheme(
        background    = "#F8FAFC",
        surface       = "#FFFFFF",
        primary       = "#6366F1",
        textPrimary   = "#1E293B",
        textSecondary = "#64748B",
        textHint      = "#94A3B8",
        border        = "#CBD5E1",
        inputBg       = "#F1F5F9",
        selectedBg    = "#EEF2FF",
    )

    private data class ColorScheme(
        val background: String,
        val surface: String,
        val primary: String,
        val textPrimary: String,
        val textSecondary: String,
        val textHint: String,
        val border: String,
        val inputBg: String,
        val selectedBg: String,
    )

    private data class Quad(
        val status: String,
        val dotColor: String,
        val btnText: String,
        val btnColor: String
    )
}
