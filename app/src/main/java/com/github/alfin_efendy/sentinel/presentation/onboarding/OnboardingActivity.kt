package com.github.alfin_efendy.sentinel.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.github.alfin_efendy.sentinel.presentation.common.PermissionHelper
import com.github.alfin_efendy.sentinel.presentation.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels()
    private lateinit var stepsContainer: LinearLayout
    private lateinit var continueBtn: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode()

        setContentView(buildUi())

        lifecycleScope.launch {
            viewModel.steps.collectLatest { steps ->
                stepsContainer.removeAllViews()
                steps.forEachIndexed { index, step ->
                    stepsContainer.addView(buildStepCard(index + 1, step, index))
                }
                val required = steps.dropLast(1)
                val allGranted = required.all { it.isGranted }
                continueBtn.isEnabled = allGranted
                continueBtn.background = pill(
                    if (allGranted) Color.parseColor("#6366F1") else Color.parseColor("#94A3B8"),
                    dp(14)
                )
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

    // ── UI ──────────────────────────────────────────────────────────────────────

    private fun buildUi(): View {
        val cs = colorScheme()

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

        root.addView(buildHero())

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }

        // Section label
        content.addView(TextView(this).apply {
            text = "PERMISSIONS REQUIRED"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(cs.textSecondary))
            letterSpacing = 0.12f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        })

        stepsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(stepsContainer)

        content.addView(spacer(dp(32)))

        continueBtn = TextView(this).apply {
            text = "Continue to App"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isEnabled = false
            background = pill(Color.parseColor("#94A3B8"), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { if (isEnabled) navigateToMain() }
        }
        content.addView(continueBtn)

        root.addView(content)

        return scroll
    }

    private fun buildHero(): View {
        val hero = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)
            )
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6366F1"))
            }
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.CENTER }
        }

        // Apply top inset padding so content clears the status bar
        ViewCompat.setOnApplyWindowInsetsListener(inner) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, top, 0, 0)
            insets
        }

        inner.addView(TextView(this).apply {
            text = "🛡️"
            textSize = 52f
            gravity = Gravity.CENTER
        })
        inner.addView(TextView(this).apply {
            text = "Sentinel"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        })
        inner.addView(TextView(this).apply {
            text = "App Monitor & Auto-Relaunch"
            textSize = 13f
            setTextColor(Color.parseColor("#C7D2FE"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        })

        hero.addView(inner)
        return hero
    }

    private fun buildStepCard(stepNumber: Int, step: PermissionStepState, index: Int): View {
        val cs = colorScheme()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(cs.surface))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.parseColor(cs.border))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Circle badge
        val badgeFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                setMargins(0, 0, dp(14), 0)
            }
        }
        val badgeBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (step.isGranted) {
                setColor(Color.parseColor("#6366F1"))
            } else {
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), Color.parseColor("#6366F1"))
            }
        }
        val badgeView = View(this).apply {
            background = badgeBg
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        badgeFrame.addView(badgeView)
        badgeFrame.addView(TextView(this).apply {
            text = if (step.isGranted) "✓" else "$stepNumber"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(
                if (step.isGranted) Color.WHITE else Color.parseColor("#6366F1")
            )
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        })
        row.addView(badgeFrame)

        // Text column
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(this).apply {
            text = step.title
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(cs.textPrimary))
        })
        textCol.addView(TextView(this).apply {
            text = step.description
            textSize = 12f
            setTextColor(Color.parseColor(cs.textSecondary))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
        row.addView(textCol)

        card.addView(row)

        // Grant button (below, full width) when not granted
        if (!step.isGranted) {
            card.addView(spacer(dp(12)))
            card.addView(TextView(this).apply {
                text = step.buttonLabel
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = pill(Color.parseColor("#6366F1"), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(40)
                )
                isClickable = true
                isFocusable = true
                setOnClickListener { handleStepGrant(index) }
            })
        }

        return card
    }

    // ── Actions ──────────────────────────────────────────────────────────────────

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

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun spacer(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }

    private fun pill(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun isDarkMode(): Boolean {
        val flags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return flags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun colorScheme(): ColorScheme = if (isDarkMode()) ColorScheme(
        surface       = "#1E293B",
        textPrimary   = "#F1F5F9",
        textSecondary = "#94A3B8",
        border        = "#334155",
    ) else ColorScheme(
        surface       = "#FFFFFF",
        textPrimary   = "#1E293B",
        textSecondary = "#64748B",
        border        = "#E2E8F0",
    )

    private data class ColorScheme(
        val surface: String,
        val textPrimary: String,
        val textSecondary: String,
        val border: String,
    )
}
