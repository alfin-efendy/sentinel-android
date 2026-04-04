package com.github.alfin_efendy.sentinel.presentation.main

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
            // Refresh affected items
            currentList.indexOfFirst { it.packageName == old }.takeIf { it >= 0 }?.let { notifyItemChanged(it) }
            currentList.indexOfFirst { it.packageName == packageName }.takeIf { it >= 0 }?.let { notifyItemChanged(it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val dp16 = (16 * resources.displayMetrics.density).toInt()
            val dp8 = (8 * resources.displayMetrics.density).toInt()
            setPadding(dp16, dp8, dp16, dp8)
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

        private val iconView: ImageView
        private val nameView: TextView
        private val pkgView: TextView

        init {
            val dp8 = (8 * row.resources.displayMetrics.density).toInt()
            val dp40 = (40 * row.resources.displayMetrics.density).toInt()

            iconView = ImageView(row.context).apply {
                layoutParams = LinearLayout.LayoutParams(dp40, dp40).apply {
                    setMargins(0, 0, dp8, 0)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            row.addView(iconView)

            val textCol = LinearLayout(row.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            nameView = TextView(row.context).apply { textSize = 15f }
            pkgView = TextView(row.context).apply {
                textSize = 11f
                setTextColor(android.graphics.Color.GRAY)
            }
            textCol.addView(nameView)
            textCol.addView(pkgView)
            row.addView(textCol)
        }

        fun bind(app: AppInfo, selectedPackage: String, onSelected: (AppInfo) -> Unit) {
            iconView.setImageDrawable(app.icon)
            nameView.text = app.label
            pkgView.text = app.packageName
            val isSelected = app.packageName == selectedPackage
            row.setBackgroundColor(
                if (isSelected) android.graphics.Color.parseColor("#E8F5E9")
                else android.graphics.Color.TRANSPARENT
            )
            row.setOnClickListener { onSelected(app) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
            override fun areContentsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName && a.label == b.label
        }
    }
}
