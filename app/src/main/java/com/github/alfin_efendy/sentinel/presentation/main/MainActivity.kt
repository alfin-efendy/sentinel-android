package com.github.alfin_efendy.sentinel.presentation.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import com.github.alfin_efendy.sentinel.presentation.common.PermissionHelper
import com.github.alfin_efendy.sentinel.presentation.onboarding.OnboardingActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: AppPickerAdapter
    private lateinit var toggleButton: Button
    private lateinit var stateLabel: TextView
    private lateinit var selectedPkgLabel: TextView
    private lateinit var deepLinkInput: EditText
    private lateinit var searchInput: EditText
    private lateinit var loadingIndicator: ProgressBar

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

    private fun buildUi() {
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp16 = (16 * resources.displayMetrics.density).toInt()

        val root = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16, dp16, dp16, dp16)
        }
        root.addView(container)
        setContentView(root)

        // Title
        container.addView(TextView(this).apply {
            text = "Sentinel"
            textSize = 26f
            setPadding(0, 0, 0, dp8 / 2)
        })
        container.addView(TextView(this).apply {
            text = "App Monitor & Auto-Relaunch"
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, dp16)
        })

        // Status
        stateLabel = TextView(this).apply {
            text = "Status: Idle"
            textSize = 14f
            setPadding(0, 0, 0, dp8)
        }
        container.addView(stateLabel)

        selectedPkgLabel = TextView(this).apply {
            text = "Target: (none)"
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, dp16)
        }
        container.addView(selectedPkgLabel)

        // Deep link
        container.addView(TextView(this).apply {
            text = "Deep Link (optional)"
            textSize = 13f
            setPadding(0, 0, 0, dp8 / 2)
        })
        deepLinkInput = EditText(this).apply {
            hint = "e.g. roblox://placeId=<id> or https://www.roblox.com/games/start?placeId=<id>"
            textSize = 13f
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp16) }
            layoutParams = params
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = viewModel.setDeepLink(s.toString())
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        container.addView(deepLinkInput)

        // Single toggle button
        toggleButton = Button(this).apply {
            text = "Start Monitoring"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp16) }
            setOnClickListener {
                if (viewModel.monitoringState.value.isActive) {
                    viewModel.stopMonitoring()
                } else {
                    viewModel.startMonitoring()
                }
            }
        }
        container.addView(toggleButton)

        // App picker
        container.addView(TextView(this).apply {
            text = "Select Target App"
            textSize = 15f
            setPadding(0, 0, 0, dp8)
        })

        searchInput = EditText(this).apply {
            hint = "Search apps..."
            textSize = 14f
            setSingleLine(true)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp8) }
            layoutParams = params
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = viewModel.setSearchQuery(s.toString())
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        container.addView(searchInput)

        loadingIndicator = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }
        container.addView(loadingIndicator)

        adapter = AppPickerAdapter { app ->
            viewModel.selectApp(app)
        }

        val recycler = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (400 * resources.displayMetrics.density).toInt()
            )
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL))
            isNestedScrollingEnabled = false
        }
        container.addView(recycler)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.installedApps.collectLatest { apps ->
                adapter.submitList(apps)
            }
        }
        lifecycleScope.launch {
            viewModel.isLoadingApps.collectLatest { loading ->
                loadingIndicator.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.config.collectLatest { config ->
                selectedPkgLabel.text = "Target: ${config.packageName.ifBlank { "(none)" }}"
                adapter.setSelected(config.packageName)
                if (deepLinkInput.text.toString() != (config.deepLinkUrl ?: "")) {
                    deepLinkInput.setText(config.deepLinkUrl ?: "")
                }
            }
        }
        lifecycleScope.launch {
            viewModel.monitoringState.collectLatest { state ->
                updateStateUi(state)
            }
        }
        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { msg ->
                msg?.let {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Error")
                        .setMessage(it)
                        .setPositiveButton("OK") { _, _ -> viewModel.clearError() }
                        .show()
                }
            }
        }
    }

    private fun updateStateUi(state: MonitoringState) {
        if (state.isActive) {
            toggleButton.text = "Stop Monitoring"
            toggleButton.setBackgroundColor(Color.parseColor("#F44336"))
            toggleButton.setTextColor(Color.WHITE)
        } else {
            toggleButton.text = "Start Monitoring"
            toggleButton.setBackgroundColor(Color.parseColor("#4CAF50"))
            toggleButton.setTextColor(Color.WHITE)
        }

        val (label, color) = when (state) {
            is MonitoringState.Idle -> "Status: Idle" to Color.GRAY
            is MonitoringState.Monitoring -> "Status: Monitoring" to Color.parseColor("#4CAF50")
            is MonitoringState.Relaunching -> "Status: Relaunching (attempt ${state.attemptCount})" to Color.parseColor("#FF9800")
            is MonitoringState.GracePeriod -> "Status: Grace Period" to Color.parseColor("#FFC107")
            is MonitoringState.Paused -> "Status: Paused" to Color.parseColor("#9E9E9E")
        }
        stateLabel.text = label
        stateLabel.setTextColor(color)
    }
}
