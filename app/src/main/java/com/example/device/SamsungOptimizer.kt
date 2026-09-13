package com.example.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.lang.reflect.Method

object SamsungOptimizer {

    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.contains("samsung", ignoreCase = true) ||
                Build.BRAND.contains("samsung", ignoreCase = true)
    }

    fun getOneUiVersion(): String? {
        if (!isSamsungDevice()) return null
        return try {
            val semPlatformClass = Class.forName("com.samsung.android.os.SemPlatform")
            val getVersionMethod: Method = semPlatformClass.getMethod("getPlatformVersion")
            val version = getVersionMethod.invoke(null) as? String
            version?.let { "One UI $it" }
        } catch (_: Exception) {
            try {
                val propClass = Class.forName("android.os.SystemProperties")
                val getMethod: Method = propClass.getMethod("get", String::class.java)
                val sepVersion = getMethod.invoke(null, "ro.build.version.sep") as? String
                if (!sepVersion.isNullOrEmpty()) "Sep $sepVersion" else "Supported"
            } catch (_: Exception) {
                "Supported"
            }
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    fun openSamsungDeviceCare(context: Context): Boolean {
        val intents = listOf(
            Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            Intent().setClassName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.RamActivity"),
            Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.ui.dashboard.DashboardActivity"),
            Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
        )

        for (intent in intents) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }
        return false
    }
}
