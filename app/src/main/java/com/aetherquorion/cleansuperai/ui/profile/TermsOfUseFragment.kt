package com.aetherquorion.cleansuperai.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.aetherquorion.cleansuperai.R

class TermsOfUseFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.color.bg_primary)
        }
        root.addView(Button(requireContext()).apply {
            text = getString(R.string.detail_back)
            setOnClickListener { parentFragmentManager.popBackStack() }
        })
        root.addView(WebView(requireContext()).apply {
            settings.javaScriptEnabled = false
            loadUrl("file:///android_asset/terms_of_use.html")
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        })
        return root
    }
}
