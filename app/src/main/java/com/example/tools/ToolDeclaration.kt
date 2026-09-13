package com.example.tools

import org.json.JSONArray
import org.json.JSONObject

object ToolDeclarations {

    fun getGeminiToolDeclarations(): JSONArray {
        val functionDeclarations = JSONArray()

        // 1. openApp
        functionDeclarations.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Opens an installed Android app on the phone such as WhatsApp, YouTube, Chrome, Camera, Settings, etc.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the app in English or Bangla (e.g. 'WhatsApp', 'YouTube', 'Chrome', 'Camera', 'হোয়াটসঅ্যাপ', 'সেটিংস')")
                    })
                })
                put("required", JSONArray().put("appName"))
            })
        })

        // 2. closeApp / goHome
        functionDeclarations.put(JSONObject().apply {
            put("name", "closeApp")
            put("description", "Navigates back to the Android home screen or closes the current view.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 3. openWebsite
        functionDeclarations.put(JSONObject().apply {
            put("name", "openWebsite")
            put("description", "Opens a URL or web search query in the browser (e.g. YouTube, Google, news).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("urlOrQuery", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The website URL or search term")
                    })
                })
                put("required", JSONArray().put("urlOrQuery"))
            })
        })

        // 4. getBatteryStatus
        functionDeclarations.put(JSONObject().apply {
            put("name", "getBatteryStatus")
            put("description", "Retrieves real battery percentage, charging state, charger type, and temperature.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 5. getWifiStatus
        functionDeclarations.put(JSONObject().apply {
            put("name", "getWifiStatus")
            put("description", "Checks Wi-Fi status, connected network SSID, and signal level.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 6. getNetworkStatus
        functionDeclarations.put(JSONObject().apply {
            put("name", "getNetworkStatus")
            put("description", "Checks if phone has active internet connection and type (Wi-Fi or Mobile Cellular).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 7. getDeviceStatus
        functionDeclarations.put(JSONObject().apply {
            put("name", "getDeviceStatus")
            put("description", "Gets full device information: brand, model, Android version, One UI version, and storage.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 8. volumeControl
        functionDeclarations.put(JSONObject().apply {
            put("name", "volumeControl")
            put("description", "Controls device media volume: increase (up), decrease (down), mute, unmute, or set specific percentage.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Action to take: 'up', 'down', 'mute', 'unmute', 'set'")
                    })
                    put("percent", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Optional volume percentage 0 to 100 when action is 'set'")
                    })
                })
                put("required", JSONArray().put("action"))
            })
        })

        // 9. mediaControl
        functionDeclarations.put(JSONObject().apply {
            put("name", "mediaControl")
            put("description", "Controls background media playback: play, pause, next song, previous song, stop.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("command", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Media command: 'play', 'pause', 'next', 'previous', 'stop'")
                    })
                })
                put("required", JSONArray().put("command"))
            })
        })

        // 10. lockDevice
        functionDeclarations.put(JSONObject().apply {
            put("name", "lockDevice")
            put("description", "Locks the device screen natively using Android Accessibility Service.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 11. notificationReader
        functionDeclarations.put(JSONObject().apply {
            put("name", "notificationReader")
            put("description", "Reads aloud recent unread notifications received by the device.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("limit", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Maximum number of notifications to read (default 3)")
                    })
                })
            })
        })

        // 12. screenAnalyzer
        functionDeclarations.put(JSONObject().apply {
            put("name", "screenAnalyzer")
            put("description", "Analyzes the visible text and buttons on screen using Accessibility Service.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 13. findContact
        functionDeclarations.put(JSONObject().apply {
            put("name", "findContact")
            put("description", "Searches device contacts by name for communication.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the person (e.g. Rahim, Karim, Mom)")
                    })
                })
                put("required", JSONArray().put("name"))
            })
        })

        // 14. prepareMessage
        functionDeclarations.put(JSONObject().apply {
            put("name", "prepareMessage")
            put("description", "Prepares a WhatsApp message and prompts the user for confirmation before sending. ALWAYS call this first when user wants to send a message.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("recipient", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name or phone number of the recipient (e.g. 'Rahim')")
                    })
                    put("messageText", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Content of the message to be sent")
                    })
                    put("platform", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Messaging platform, default 'whatsapp'")
                    })
                })
                put("required", JSONArray().put("recipient").put("messageText"))
            })
        })

        // 15. openConversation
        functionDeclarations.put(JSONObject().apply {
            put("name", "openConversation")
            put("description", "Opens WhatsApp conversation directly for a contact.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Contact name or number")
                    })
                })
                put("required", JSONArray().put("contactName"))
            })
        })

        return JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations))
    }
}
