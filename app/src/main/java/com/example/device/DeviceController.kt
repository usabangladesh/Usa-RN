package com.example.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.accessibility.RashedAccessibilityService
import java.text.DecimalFormat

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val pluggedSource: String,
    val temperatureCelsius: Float,
    val health: String
)

data class WifiInfo(
    val isEnabled: Boolean,
    val isConnected: Boolean,
    val ssid: String,
    val signalLevel: Int
)

data class NetworkInfo(
    val hasInternet: Boolean,
    val connectionType: String
)

data class StorageInfo(
    val totalGB: Double,
    val freeGB: Double,
    val percentFree: Int
)

class DeviceController(private val context: Context) {

    fun getBatteryInfo(): BatteryInfo {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val pluggedSource = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Fast Charging"
            else -> "Battery"
        }

        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = temp / 10.0f

        val healthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            pluggedSource = pluggedSource,
            temperatureCelsius = tempCelsius,
            health = health
        )
    }

    fun getWifiInfo(): WifiInfo {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isEnabled = wifiManager?.isWifiEnabled ?: false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        var ssid = "Unknown"
        var signalLevel = 0
        if (isConnected && wifiManager != null) {
            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo != null) {
                val rawSsid = connectionInfo.ssid ?: ""
                ssid = if (rawSsid.startsWith("\"") && rawSsid.endsWith("\"")) {
                    rawSsid.substring(1, rawSsid.length - 1)
                } else {
                    rawSsid
                }
                signalLevel = WifiManager.calculateSignalLevel(connectionInfo.rssi, 5)
            }
        }

        return WifiInfo(
            isEnabled = isEnabled,
            isConnected = isConnected,
            ssid = ssid,
            signalLevel = signalLevel
        )
    }

    fun getNetworkInfo(): NetworkInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)

        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val type = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Cellular Data"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true -> "Bluetooth"
            else -> "Disconnected"
        }

        return NetworkInfo(hasInternet = hasInternet, connectionType = type)
    }

    fun getStorageInfo(): StorageInfo {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize

        val df = DecimalFormat("#.##")
        val totalGB = df.format(totalBytes / (1024.0 * 1024 * 1024)).toDoubleOrNull() ?: 0.0
        val freeGB = df.format(freeBytes / (1024.0 * 1024 * 1024)).toDoubleOrNull() ?: 0.0
        val pctFree = if (totalBytes > 0) ((freeBytes.toDouble() / totalBytes) * 100).toInt() else 0

        return StorageInfo(totalGB = totalGB, freeGB = freeGB, percentFree = pctFree)
    }

    fun getDeviceOverview(): String {
        val isSamsung = SamsungOptimizer.isSamsungDevice()
        val brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val androidVer = Build.VERSION.RELEASE
        val oneUiVer = if (isSamsung) SamsungOptimizer.getOneUiVersion() else null

        return buildString {
            append("$brand $model (Android $androidVer)")
            if (oneUiVer != null) {
                append(" | Samsung One UI $oneUiVer")
            }
        }
    }

    fun lockDevice(): Result<String> {
        val service = RashedAccessibilityService.instance
        return if (service != null) {
            val locked = service.lockScreen()
            if (locked) {
                Result.success("ফোনটি সফলভাবে lock করা হয়েছে (Device locked successfully).")
            } else {
                Result.failure(Exception("Lock screen action could not be performed by accessibility service."))
            }
        } else {
            Result.failure(Exception("Device lock requires Rashed Accessibility Service to be enabled in Settings."))
        }
    }
}
