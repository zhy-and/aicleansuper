package com.aetherquorion.cleansuperai.ui.common

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.aetherquorion.cleansuperai.R

fun inlineAdHost(context: Context): FrameLayout = FrameLayout(context).apply {
    id = R.id.inlineAdContainer
    visibility = View.GONE
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = (12 * resources.displayMetrics.density).toInt() }
}
