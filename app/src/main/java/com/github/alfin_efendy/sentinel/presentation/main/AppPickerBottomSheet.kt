package com.github.alfin_efendy.sentinel.presentation.main

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppPickerBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: AppPickerAdapter

    override fun onStart() {
        super.onStart()
        val d = dialog ?: return

        // Remove the dialog window's own background (outermost layer)
        d.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

        val sheet = d.findViewById<FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        // Null out backgrounds at every level so only our rounded card is visible
        sheet.background = null
        (sheet.parent as? ViewGroup)?.background = null

        BottomSheetBehavior.from(sheet).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = buildContent()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setSearchQuery("")
    }

    private fun buildContent(): View {
        val ctx = requireContext()
        val cs = colorScheme()
        fun Int.dp() = (this * ctx.resources.displayMetrics.density).toInt()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(cs.surface))
                cornerRadii = floatArrayOf(
                    24.dp().toFloat(), 24.dp().toFloat(),
                    24.dp().toFloat(), 24.dp().toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
            minimumHeight = (ctx.resources.displayMetrics.heightPixels * 0.75).toInt()
        }

        // Drag handle
        root.addView(FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 28.dp()
            )
            addView(View(ctx).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(cs.handle))
                    cornerRadius = 3.dp().toFloat()
                }
                layoutParams = FrameLayout.LayoutParams(40.dp(), 4.dp()).apply {
                    gravity = Gravity.CENTER
                }
            })
        })

        // Title
        root.addView(TextView(ctx).apply {
            text = "Select Application"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(cs.textPrimary))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(20.dp(), 0, 20.dp(), 12.dp()) }
        })

        // Search input
        root.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(cs.inputBg))
                cornerRadius = 12.dp().toFloat()
            }
            setPadding(14.dp(), 0, 14.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dp()
            ).apply { setMargins(16.dp(), 0, 16.dp(), 12.dp()) }

            addView(TextView(ctx).apply {
                text = "⌕"
                textSize = 18f
                setTextColor(Color.parseColor(cs.textHint))
                setPadding(0, 0, 8.dp(), 0)
            })
            addView(EditText(ctx).apply {
                hint = "Search apps..."
                textSize = 14f
                setTextColor(Color.parseColor(cs.textPrimary))
                setHintTextColor(Color.parseColor(cs.textHint))
                background = null
                setSingleLine(true)
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) =
                        viewModel.setSearchQuery(s?.toString() ?: "")
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                })
            })
        })

        // Thin divider
        root.addView(View(ctx).apply {
            setBackgroundColor(Color.parseColor(cs.divider))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1.dp()
            )
        })

        // App list
        adapter = AppPickerAdapter { app ->
            viewModel.selectApp(app)
            dismiss()
        }
        root.addView(RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = this@AppPickerBottomSheet.adapter
            isNestedScrollingEnabled = true
            setPadding(0, 4.dp(), 0, 24.dp())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })

        return root
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.installedApps.collectLatest { adapter.submitList(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.config.collectLatest { adapter.setSelected(it.packageName) }
        }
    }

    private fun isDarkMode(): Boolean {
        val flags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return flags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun colorScheme(): ColorScheme = if (isDarkMode()) ColorScheme(
        surface     = "#1E293B",
        textPrimary = "#F1F5F9",
        textHint    = "#475569",
        inputBg     = "#0F172A",
        divider     = "#334155",
        handle      = "#334155",
    ) else ColorScheme(
        surface     = "#FFFFFF",
        textPrimary = "#1E293B",
        textHint    = "#94A3B8",
        inputBg     = "#F1F5F9",
        divider     = "#F1F5F9",
        handle      = "#CBD5E1",
    )

    private data class ColorScheme(
        val surface: String,
        val textPrimary: String,
        val textHint: String,
        val inputBg: String,
        val divider: String,
        val handle: String,
    )

    companion object {
        const val TAG = "AppPickerBottomSheet"
    }
}
