package com.example.tools

import org.json.JSONArray
import org.json.JSONObject

object ToolDeclarations {

    fun getGeminiToolDeclarations(): JSONArray {
        val functionDeclarations = JSONArray()

        fun addTool(name: String, description: String, required: List<String> = emptyList(), properties: Map<String, Pair<String, String>> = emptyMap()) {
            val propsObj = JSONObject()
            for ((propName, pair) in properties) {
                val (type, desc) = pair
                propsObj.put(propName, JSONObject().apply {
                    put("type", type)
                    put("description", desc)
                })
            }
            val reqArray = JSONArray()
            required.forEach { reqArray.put(it) }

            functionDeclarations.put(JSONObject().apply {
                put("name", name)
                put("description", description)
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", propsObj)
                    if (reqArray.length() > 0) {
                        put("required", reqArray)
                    }
                })
            })
        }

        // Section 21 Tools:
        // Browser & Web Actions
        addTool(
            "openWebsite",
            "Opens a website URL in the device web browser (e.g. 'https://google.com', 'youtube.com', 'x.com', 'github.com').",
            listOf("url"),
            mapOf("url" to Pair("STRING", "Website address, domain, or URL to open in browser"))
        )
        addTool(
            "searchWeb",
            "Executes an instant web search for the given query and shows the browser search page.",
            listOf("query"),
            mapOf("query" to Pair("STRING", "Search terms or query to find online"))
        )

        // App Control
        addTool(
            "openApp",
            "Opens an installed Android app on the phone by name (e.g. WhatsApp, YouTube, Chrome, Settings).",
            listOf("appName"),
            mapOf("appName" to Pair("STRING", "Name of the app (e.g. 'YouTube', 'WhatsApp', 'Chrome', 'Camera')"))
        )
        addTool(
            "findApp",
            "Checks if an app is installed and returns its package details.",
            listOf("appName"),
            mapOf("appName" to Pair("STRING", "App name to look for"))
        )
        addTool(
            "focusApp",
            "Brings an already running app to foreground focus.",
            listOf("appName"),
            mapOf("appName" to Pair("STRING", "App name to bring to foreground"))
        )

        // YouTube Control
        addTool(
            "searchYouTube",
            "Searches for a video or query on YouTube and displays results.",
            listOf("query"),
            mapOf("query" to Pair("STRING", "Search query or video title (e.g. 'Ronaldo', 'Messi vs Ronaldo')"))
        )
        addTool(
            "playYouTubeVideo",
            "Plays the requested video or matching search result on YouTube.",
            listOf("videoTitle"),
            mapOf("videoTitle" to Pair("STRING", "Title or description of the video to play"))
        )

        // WhatsApp / Messaging
        addTool(
            "findContact",
            "Searches device contacts by name.",
            listOf("name"),
            mapOf("name" to Pair("STRING", "Name of the contact to find"))
        )
        addTool(
            "openWhatsAppConversation",
            "Opens WhatsApp conversation directly for a given contact or number.",
            listOf("contactName"),
            mapOf(
                "contactName" to Pair("STRING", "Contact name or phone number"),
                "message" to Pair("STRING", "Optional draft message")
            )
        )
        addTool(
            "typeText",
            "Types text into currently focused editable field or input box.",
            listOf("text"),
            mapOf("text" to Pair("STRING", "Text string to type into the field"))
        )
        addTool(
            "prepareMessage",
            "Prepares a WhatsApp message and prompts user confirmation. ALWAYS call this before sending consequential messages.",
            listOf("recipient", "messageText"),
            mapOf(
                "recipient" to Pair("STRING", "Contact name or phone number"),
                "messageText" to Pair("STRING", "Text message body")
            )
        )
        addTool(
            "sendMessage",
            "Sends the prepared WhatsApp message after user approval.",
            listOf("recipient", "messageText"),
            mapOf(
                "recipient" to Pair("STRING", "Contact name or phone number"),
                "messageText" to Pair("STRING", "Text message body")
            )
        )

        // Volume & Media
        addTool(
            "volumeControl",
            "Controls device volume: increase (up), decrease (down), mute, unmute, or set percent.",
            listOf("action"),
            mapOf(
                "action" to Pair("STRING", "Action: 'up', 'down', 'mute', 'unmute', 'set'"),
                "percent" to Pair("INTEGER", "Volume percentage 0 to 100 when action is set")
            )
        )
        addTool(
            "mediaControl",
            "Controls media playback: 'play', 'pause', 'next', 'previous', 'stop'.",
            listOf("command"),
            mapOf("command" to Pair("STRING", "Command: 'play', 'pause', 'next', 'previous', 'stop'"))
        )

        // Device Status
        addTool("getBatteryStatus", "Returns real battery charge level, charging status, source, and temperature.")
        addTool("getNetworkStatus", "Checks if device has active internet and connection type (Wi-Fi or Mobile Data).")
        addTool("getWifiStatus", "Returns Wi-Fi status, connected network SSID, and signal quality.")
        addTool("getStorageStatus", "Returns device internal storage total GB, free GB, and percentage free.")

        // Settings & Hardware
        addTool(
            "openSettings",
            "Opens device settings screen.",
            emptyList(),
            mapOf("type" to Pair("STRING", "Optional settings type: 'main', 'wifi', 'bluetooth', 'sound', 'display'"))
        )
        addTool("openWifiSettings", "Opens the system Wi-Fi settings page.")
        addTool("openBluetoothSettings", "Opens the system Bluetooth settings page.")
        addTool("openCamera", "Opens the device camera viewfinder.")
        addTool("lockDevice", "Locks the phone screen securely via Accessibility service.")

        // Accessibility Engine Tools
        addTool(
            "accessibilityFind",
            "Finds visible text or interactive elements on current screen using Accessibility.",
            listOf("text"),
            mapOf("text" to Pair("STRING", "Text or description to look for on screen"))
        )
        addTool(
            "accessibilityClick",
            "Clicks a visible element on screen matching the given text or description.",
            listOf("targetText"),
            mapOf("targetText" to Pair("STRING", "Text or content description of element to click"))
        )
        addTool(
            "accessibilitySetText",
            "Enters text into currently active or matching editable field.",
            listOf("text"),
            mapOf("text" to Pair("STRING", "Text to insert"))
        )
        addTool(
            "accessibilityScroll",
            "Scrolls the current screen container up or down.",
            listOf("direction"),
            mapOf("direction" to Pair("STRING", "'up' or 'down'"))
        )
        addTool("accessibilityBack", "Executes real Android Back action via Accessibility.")

        return JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations))
    }
}
