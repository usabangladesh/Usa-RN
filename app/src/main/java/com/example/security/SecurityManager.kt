package com.example.security

import com.example.permissions.PermissionLevel
import com.example.permissions.ToolSecurityPolicy

class SecurityManager {

    private val toolPolicies = mapOf(
        "openApp" to ToolSecurityPolicy("openApp", PermissionLevel.SAFE, "Open installed app"),
        "closeApp" to ToolSecurityPolicy("closeApp", PermissionLevel.SAFE, "Go to home screen"),
        "focusApp" to ToolSecurityPolicy("focusApp", PermissionLevel.SAFE, "Focus target application"),
        "openWebsite" to ToolSecurityPolicy("openWebsite", PermissionLevel.SAFE, "Open web URL or search"),
        "getDeviceStatus" to ToolSecurityPolicy("getDeviceStatus", PermissionLevel.SAFE, "Read hardware and OS overview"),
        "getBatteryStatus" to ToolSecurityPolicy("getBatteryStatus", PermissionLevel.SAFE, "Read battery level & charging state"),
        "getNetworkStatus" to ToolSecurityPolicy("getNetworkStatus", PermissionLevel.SAFE, "Check connectivity"),
        "getWifiStatus" to ToolSecurityPolicy("getWifiStatus", PermissionLevel.SAFE, "Check Wi-Fi network status"),
        "mediaControl" to ToolSecurityPolicy("mediaControl", PermissionLevel.SAFE, "Playback control"),
        "volumeControl" to ToolSecurityPolicy("volumeControl", PermissionLevel.SAFE, "Adjust stream volume"),
        "lockDevice" to ToolSecurityPolicy("lockDevice", PermissionLevel.SAFE, "Legitimate screen lock via Accessibility"),
        "notificationReader" to ToolSecurityPolicy("notificationReader", PermissionLevel.SAFE, "Summarize user permitted notifications"),
        "screenAnalyzer" to ToolSecurityPolicy("screenAnalyzer", PermissionLevel.SAFE, "Analyze visible screen UI"),
        "findContact" to ToolSecurityPolicy("findContact", PermissionLevel.SAFE, "Look up contact for messaging/calls"),
        "openConversation" to ToolSecurityPolicy("openConversation", PermissionLevel.SAFE, "Open messaging thread"),
        "prepareMessage" to ToolSecurityPolicy("prepareMessage", PermissionLevel.SAFE, "Draft message and ask confirmation"),
        "sendMessage" to ToolSecurityPolicy("sendMessage", PermissionLevel.CONFIRMATION_REQUIRED, "Send message to contact"),
        "extractPassword" to ToolSecurityPolicy("extractPassword", PermissionLevel.RESTRICTED, "Prohibited credential access"),
        "secretRecording" to ToolSecurityPolicy("secretRecording", PermissionLevel.RESTRICTED, "Prohibited unauthorized capture")
    )

    fun getPolicy(toolName: String): ToolSecurityPolicy {
        return toolPolicies[toolName] ?: ToolSecurityPolicy(
            toolName = toolName,
            level = PermissionLevel.SAFE,
            description = "General action"
        )
    }

    fun isRestricted(toolName: String, query: String = ""): Boolean {
        val policy = getPolicy(toolName)
        if (policy.level == PermissionLevel.RESTRICTED) return true

        val lower = query.lowercase()
        val dangerousKeywords = listOf(
            "password", "pin", "credential", "bypass lock", "steal", "hack", "spy",
            "পাসওয়ার্ড", "গোপন তথ্য", "হ্যাক"
        )
        return dangerousKeywords.any { lower.contains(it) }
    }
}
