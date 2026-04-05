package com.github.alfin_efendy.sentinel.presentation.main

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.alfin_efendy.sentinel.domain.model.AppInfo

class AppPickerAdapter(
    private val onSelected: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppPickerAdapter.ViewHolder>(DIFF) {

    private var selectedPackage: String = ""

    fun setSelected(packageName: String) {
        val old = selectedPackage
        selectedPackage = packageName
        if (old != packageName) {
            currentList.indexOfFirst { it.packageName == old }
                .takeIf { it >= 0 }?.let { notifyItemChanged(it) }
            currentList.indexOfFirst { it.packageName == packageName }
                .takeIf { it >= 0 }?.let { notifyItemChanged(it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ctx = parent.context
        fun Int.dp() = (this * ctx.resources.displayMetrics.density).toInt()

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20.dp(), 12.dp(), 20.dp(), 12.dp())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return ViewHolder(row)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), selectedPackage, onSelected)
    }

    class ViewHolder(private val row: LinearLayout) : RecyclerView.ViewHolder(row) {

        private val ctx: Context get() = row.context
        private val iconView: ImageView
        private val iconContainer: FrameLayout
        private val nameView: TextView
        private val pkgView: TextView
        private val checkView: TextView

        init {
            fun Int.dp() = (this * ctx.resources.displayMetrics.density).toInt()

            iconContainer = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(44.dp(), 44.dp()).apply {
                    setMargins(0, 0, 14.dp(), 0)
                }
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F1F5F9"))
                    cornerRadius = 10.dp().toFloat()
                }
            }
            iconView = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    28.dp(), 28.dp()
                ).apply { gravity = Gravity.CENTER }
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(0, 0, 0, 0)
            }
            iconContainer.addView(iconView)
            row.addView(iconContainer)

            val textCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            nameView = TextView(ctx).apply {
                textSize = 15f
                setTextColor(Color.parseColor("#1E293B"))
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            pkgView = TextView(ctx).apply {
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            textCol.addView(nameView)
            textCol.addView(pkgView)
            row.addView(textCol)

            checkView = TextView(ctx).apply {
                text = "✓"
                textSize = 16f
                setTextColor(Color.parseColor("#6366F1"))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    fun Int.dp() = (this * ctx.resources.displayMetrics.density).toInt()
                    setMargins(12.dp(), 0, 0, 0)
                }
                visibility = android.view.View.GONE
            }
            row.addView(checkView)
        }

        fun bind(app: AppInfo, selectedPackage: String, onSelected: (AppInfo) -> Unit) {
            fun Int.dp() = (this * ctx.resources.displayMetrics.density).toInt()
            val cs = colorScheme()

            iconView.setImageDrawable(app.icon)
            nameView.text = app.label
            nameView.setTextColor(Color.parseColor(cs.textPrimary))
            pkgView.text = app.packageName
            pkgView.setTextColor(Color.parseColor(cs.textHint))

            // Icon container background
            (iconContainer.background as? GradientDrawable)
                ?.setColor(Color.parseColor(cs.iconBg))

            val isSelected = app.packageName == selectedPackage
            checkView.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
            row.setBackgroundColor(
                if (isSelected) Color.parseColor(cs.selectedBg) else Color.TRANSPARENT
            )
            // Checkmark tint adapts to primary color
            checkView.setTextColor(Color.parseColor(cs.primary))

            row.setOnClickListener { onSelected(app) }
        }

        private fun isDarkMode(): Boolean {
            val flags = ctx.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
            return flags == Configuration.UI_MODE_NIGHT_YES
        }

        private fun colorScheme(): ColorScheme = if (isDarkMode()) ColorScheme(
            textPrimary = "#F1F5F9",
            textHint    = "#64748B",
            iconBg      = "#0F172A",
            selectedBg  = "#1E1B4B",
            primary     = "#818CF8",
        ) else ColorScheme(
            textPrimary = "#1E293B",
            textHint    = "#94A3B8",
            iconBg      = "#F1F5F9",
            selectedBg  = "#EEF2FF",
            primary     = "#6366F1",
        )

        private data class ColorScheme(
            val textPrimary: String,
            val textHint: String,
            val iconBg: String,
            val selectedBg: String,
            val primary: String,
        )
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
            override fun areContentsTheSame(a: AppInfo, b: AppInfo) =
                a.packageName == b.packageName && a.label == b.label
        }
    }
}
