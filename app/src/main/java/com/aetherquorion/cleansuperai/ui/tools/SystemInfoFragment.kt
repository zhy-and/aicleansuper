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
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.ui.common.ParityUi
import com.aetherquorion.cleansuperai.ui.common.TopFilterUi
import com.aetherquorion.cleansuperai.ui.flow.CleanupReportFragment
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
        content.addView(TopFilterUi.toolbar(
            context,
            getString(if (battery) R.string.tool_battery_info else R.string.tool_device_info),
        ) { parentFragmentManager.popBackStack() })
        if (battery) addBatteryRows(content) else addDeviceRows(content)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.bg_primary))
        }
        root.addView(ScrollView(context).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(ParityUi.button(context, ParityUi.ButtonKind.PRIMARY).apply {
            setText(R.string.action_got_it_short)
            setOnClickListener {
                (activity as? MainActivity)?.openDetail(
                    CleanupReportFragment.infoOnly(), "info_report")
            }
        }, LinearLayout.LayoutParams(-1, dp(56)).apply {
            marginStart = dp(20); marginEnd = dp(20); bottomMargin = dp(20)
        })
        return root
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
        row(parent, getString(R.string.sys_level), if (level >= 0 && scale > 0) "${level * 100 / scale}%" else getString(R.string.sys_unknown))
        row(parent, getString(R.string.sys_status), when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> getString(R.string.sys_charging)
            BatteryManager.BATTERY_STATUS_FULL -> getString(R.string.sys_full)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> getString(R.string.sys_discharging)
            else -> getString(R.string.sys_unknown)
        })
        row(parent, getString(R.string.sys_temperature), if (temperature >= 0) "${temperature / 10f}°C" else getString(R.string.sys_unknown))
        row(parent, getString(R.string.sys_voltage), if (voltage >= 0) "$voltage mV" else getString(R.string.sys_unknown))
        val current = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        row(parent, getString(R.string.sys_current), if (current != Int.MIN_VALUE) "$current µA" else getString(R.string.sys_unknown))
        row(parent, getString(R.string.sys_health), when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> getString(R.string.sys_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> getString(R.string.sys_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> getString(R.string.sys_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> getString(R.string.sys_over_voltage)
            BatteryManager.BATTERY_HEALTH_COLD -> getString(R.string.sys_cold)
            else -> getString(R.string.sys_unknown)
        })
        row(parent, getString(R.string.sys_type), technology.ifBlank { getString(R.string.sys_unknown) })
        val capacity = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        row(parent, getString(R.string.sys_capacity), if (capacity >= 0) "$capacity%" else getString(R.string.sys_unknown))
    }

    private fun addDeviceRows(parent: LinearLayout) {
        row(parent, getString(R.string.sys_android_version), Build.VERSION.RELEASE)
        row(parent, getString(R.string.sys_model), Build.MODEL)
        row(parent, getString(R.string.sys_brand), Build.BRAND)
        row(parent, getString(R.string.sys_manufacturer), Build.MANUFACTURER)
        val wifi = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val connection = runCatching { wifi?.connectionInfo }.getOrNull()
        row(parent, getString(R.string.sys_wifi_name), connection?.ssid?.takeUnless { it.isBlank() || it == WifiManager.UNKNOWN_SSID } ?: getString(R.string.sys_unknown))
        row(parent, getString(R.string.sys_wifi_ip), runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }?.hostAddress
        }.getOrNull() ?: getString(R.string.sys_unknown))
        row(parent, getString(R.string.sys_wifi_speed), connection?.linkSpeed?.takeIf { it > 0 }?.let { "$it Mbps" } ?: getString(R.string.sys_unknown))
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
