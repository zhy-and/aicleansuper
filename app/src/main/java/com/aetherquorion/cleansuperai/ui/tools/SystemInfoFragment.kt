package com.aetherquorion.cleansuperai.ui.tools

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.ui.common.ParityUi
import java.net.Inet4Address
import java.net.NetworkInterface

class SystemInfoFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val context = requireContext()
        val battery = requireArguments().getBoolean(ARG_BATTERY)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(28))
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_primary))
        }
        content.addView(ParityUi.back(context) { parentFragmentManager.popBackStack() })
        content.addView(ParityUi.title(context, if (battery) "Battery info" else "Device info"))
        if (battery) addBatteryRows(content) else addDeviceRows(content)
        return ScrollView(context).apply { addView(content) }
    }

    private fun addBatteryRows(parent: LinearLayout) {
        val sticky = requireContext().registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val temperature = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val voltage = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val health = sticky?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val technology = sticky?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty()
        val manager = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        row(parent, "Level", if (level >= 0 && scale > 0) "${level * 100 / scale}%" else "Unknown")
        row(parent, "Status", when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            else -> "Unknown"
        })
        row(parent, "Temperature", if (temperature >= 0) "${temperature / 10f}°C" else "Unknown")
        row(parent, "Voltage", if (voltage >= 0) "$voltage mV" else "Unknown")
        val current = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        row(parent, "Current", if (current != Int.MIN_VALUE) "$current µA" else "Unknown")
        row(parent, "Health", when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        })
        row(parent, "Type", technology.ifBlank { "Unknown" })
        val capacity = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        row(parent, "Battery capacity", if (capacity >= 0) "$capacity%" else "Unknown")
    }

    private fun addDeviceRows(parent: LinearLayout) {
        row(parent, "Android version", Build.VERSION.RELEASE)
        row(parent, "Model", Build.MODEL)
        row(parent, "Brand", Build.BRAND)
        row(parent, "Manufacturer", Build.MANUFACTURER)
        val wifi = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val connection = runCatching { wifi?.connectionInfo }.getOrNull()
        row(parent, "Wi-Fi name", connection?.ssid?.takeUnless { it.isBlank() || it == WifiManager.UNKNOWN_SSID } ?: "Unknown")
        row(parent, "Wi-Fi IP", runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }?.hostAddress
        }.getOrNull() ?: "Unknown")
        row(parent, "Wi-Fi speed", connection?.linkSpeed?.takeIf { it > 0 }?.let { "$it Mbps" } ?: "Unknown")
    }

    private fun row(parent: LinearLayout, title: String, value: String) {
        val context = requireContext()
        parent.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundResource(R.drawable.bg_button_secondary)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
            addView(text(context, title, 13).apply { setTextColor(ContextCompat.getColor(context, R.color.text_secondary)) })
            addView(text(context, value, 18))
        })
    }

    private fun text(context: Context, value: String, size: Int) = TextView(context).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_BATTERY = "battery"
        fun battery() = SystemInfoFragment().apply { arguments = Bundle().apply { putBoolean(ARG_BATTERY, true) } }
        fun device() = SystemInfoFragment().apply { arguments = Bundle().apply { putBoolean(ARG_BATTERY, false) } }
    }
}
