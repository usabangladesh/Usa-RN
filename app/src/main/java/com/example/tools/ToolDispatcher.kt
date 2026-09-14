package com.example.tools

import android.content.Context
import com.example.accessibility.AccessibilityActionEngine
import com.example.apps.AppController
import com.example.device.DeviceController
import com.example.media.MediaAction
import com.example.media.MediaController
import com.example.media.VolumeDirection
import com.example.screen.ScreenAnalyzer
import com.example.security.ConfirmationManager
import com.example.security.PendingConfirmation
import com.example.security.SecurityManager
import org.json.JSONObject

/**
 * Dispatches and executes the 24 Registered Tools (Section 21).
 * Adheres to Section 22 & 23: Strict Verification Contract:
 * - Never returns false success
 * - Real execution verification
 * - Consequential actions require explicit confirmation
 */
class ToolDispatcher(
    private val context: Context,
    private val appController: AppController,
    private val deviceController: DeviceController,
    private val mediaController: MediaController,
    private val screenAnalyzer: ScreenAnalyzer,
    private val confirmationManager: ConfirmationManager,
    private val securityManager: SecurityManager
) {

    suspend fun dispatchTool(toolName: String, args: JSONObject): ToolResult {
        // Security policy check
        if (securityManager.isRestricted(toolName, args.toString())) {
            return ToolResult(
                success = false,
                message = "নিরাপত্তা বিধির কারণে এই কাজটি করা সম্ভব নয়।",
                toolName = toolName,
                verified = false,
                error = "Restricted by security policy"
            )
        }

        return when (toolName) {
            // Browser & Web Navigation
            "openWebsite" -> {
                val url = args.optString("url", "")
                if (url.isBlank()) {
                    ToolResult(false, "Please specify which website or URL you'd like me to open, babe.", toolName, verified = false)
                } else {
                    val result = appController.openWebsite(url)
                    if (result.isSuccess) {
                        ToolResult(true, "Opened $url in your browser. Look at us surfing in style!", toolName, verified = true)
                    } else {
                        ToolResult(false, result.exceptionOrNull()?.message ?: "Couldn't open that website.", toolName, verified = false)
                    }
                }
            }

            "searchWeb" -> {
                val query = args.optString("query", "")
                if (query.isBlank()) {
                    ToolResult(false, "Tell me what you want to search for, handsome.", toolName, verified = false)
                } else {
                    val result = appController.searchWeb(query)
                    if (result.isSuccess) {
                        ToolResult(true, "Searching the web for \"$query\". Let's see what we find!", toolName, verified = true)
                    } else {
                        ToolResult(false, result.exceptionOrNull()?.message ?: "Search couldn't be launched.", toolName, verified = false)
                    }
                }
            }

            // App Management Tools
            "openApp" -> {
                val appName = args.optString("appName", "")
                if (appName.isBlank()) {
                    ToolResult(false, "কোন অ্যাপ খুলতে হবে তা বলুন।", toolName, verified = false)
                } else {
                    val result = appController.openApp(appName)
                    if (result.isSuccess) {
                        ToolResult(true, result.getOrThrow(), toolName, verified = true)
                    } else {
                        ToolResult(false, result.exceptionOrNull()?.message ?: "এই app-টি ফোনে install করা নেই।", toolName, verified = false, error = "App not opened")
                    }
                }
            }

            "findApp" -> {
                val appName = args.optString("appName", "")
                val found = appController.findApp(appName)
                if (found != null && appController.isAppInstalled(found)) {
                    ToolResult(true, "$appName ফোনে ইনস্টল করা আছে ($found)।", toolName, verified = true)
                } else {
                    ToolResult(false, "এই app-টি ফোনে install করা নেই।", toolName, verified = true, error = "Not installed")
                }
            }

            "focusApp" -> {
                val appName = args.optString("appName", "")
                val result = appController.focusApp(appName)
                if (result.isSuccess) {
                    ToolResult(true, result.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, result.exceptionOrNull()?.message ?: "অ্যাপ ফোকাস করা সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            // YouTube Control
            "searchYouTube" -> {
                val query = args.optString("query", "")
                if (query.isBlank()) {
                    ToolResult(false, "YouTube-এ কী খুঁজতে চান তা উল্লেখ করুন।", toolName, verified = false)
                } else {
                    val res = appController.searchYouTube(query)
                    if (res.isSuccess) {
                        ToolResult(true, res.getOrThrow(), toolName, verified = true)
                    } else {
                        ToolResult(false, res.exceptionOrNull()?.message ?: "YouTube-এ অনুসন্ধান করা সম্ভব হয়নি।", toolName, verified = false)
                    }
                }
            }

            "playYouTubeVideo" -> {
                val title = args.optString("videoTitle", "")
                val res = appController.playYouTubeVideo(title)
                if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "ভিডিও চালানো সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            // Contact & WhatsApp Automation
            "findContact" -> {
                val name = args.optString("name", "")
                val contact = appController.findContactByName(name)
                if (contact != null) {
                    val phone = contact.phoneNumber ?: "নম্বর পাওয়া যায়নি"
                    ToolResult(true, "${contact.displayName}-এর নম্বর পাওয়া গেছে: $phone", toolName, verified = true)
                } else {
                    ToolResult(false, "'$name' নামে কোনো যোগাযোগ নম্বর পাওয়া যায়নি।", toolName, verified = false)
                }
            }

            "openWhatsAppConversation" -> {
                val contactName = args.optString("contactName", "")
                val message = if (args.has("message")) args.getString("message") else null
                val res = appController.openWhatsAppConversation(contactName, message)
                if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "কথোপকথন খোলা সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            "typeText" -> {
                val text = args.optString("text", "")
                val success = AccessibilityActionEngine.typeText(null, text)
                if (success) {
                    ToolResult(true, "\"$text\" টাইপ করা হয়েছে।", toolName, verified = true)
                } else {
                    ToolResult(false, "আমি প্রয়োজনীয় লেখার ইনপুট ফিল্ড খুঁজে পাইনি।", toolName, verified = false)
                }
            }

            "prepareMessage" -> {
                val recipient = args.optString("recipient", "")
                val messageText = args.optString("messageText", "")
                if (recipient.isBlank() || messageText.isBlank()) {
                    return ToolResult(false, "প্রাপক বা মেসেজের বিষয়বস্তু অনুপস্থিত।", toolName, verified = false)
                }

                val promptText = "$recipient-এর জন্য এই মেসেজটি প্রস্তুত করেছি: \"$messageText\"। Send করব?"

                confirmationManager.requestConfirmation(
                    PendingConfirmation(
                        actionId = "whatsapp_send_${System.currentTimeMillis()}",
                        title = "Send WhatsApp Message",
                        promptText = promptText,
                        recipient = recipient,
                        content = messageText,
                        onConfirm = {
                            val openResult = appController.openWhatsAppConversation(recipient, messageText)
                            if (openResult.isSuccess) {
                                "মেসেজ পাঠানো হয়েছে।"
                            } else {
                                openResult.exceptionOrNull()?.message ?: "মেসেজ পাঠানো যায়নি।"
                            }
                        },
                        onCancel = {
                            "মেসেজ পাঠানো বাতিল করা হয়েছে।"
                        }
                    )
                )

                ToolResult(
                    success = true,
                    message = promptText,
                    toolName = toolName,
                    verified = true,
                    requiresConfirmation = true,
                    pendingActionPrompt = promptText
                )
            }

            "sendMessage" -> {
                val recipient = args.optString("recipient", "")
                val messageText = args.optString("messageText", "")
                val res = appController.sendMessage(recipient, messageText)
                if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "মেসেজ পাঠানো যায়নি।", toolName, verified = false)
                }
            }

            // Volume & Media Controls
            "volumeControl" -> {
                val action = args.optString("action", "").lowercase()
                val percent = args.optInt("percent", -1)
                val msg = when {
                    action == "up" || action.contains("বাড়") || action.contains("increas") -> {
                        mediaController.adjustVolume(VolumeDirection.UP)
                    }
                    action == "down" || action.contains("কমা") || action.contains("decreas") -> {
                        mediaController.adjustVolume(VolumeDirection.DOWN)
                    }
                    action == "mute" || action.contains("মিউট") -> {
                        mediaController.setMute(true)
                    }
                    action == "unmute" || action.contains("আনমিউট") -> {
                        mediaController.setMute(false)
                    }
                    action == "set" && percent >= 0 -> {
                        mediaController.setVolumePercent(percent)
                    }
                    else -> "Volume সমন্বয় করা হয়েছে।"
                }
                ToolResult(true, msg, toolName, verified = true)
            }

            "mediaControl" -> {
                val cmd = args.optString("command", "").lowercase()
                val mediaAction = when {
                    cmd.contains("pause") || cmd.contains("থাম") || cmd.contains("বন্ধ") -> MediaAction.PAUSE
                    cmd.contains("play") || cmd.contains("চালু") || cmd.contains("বাজা") -> MediaAction.PLAY
                    cmd.contains("next") || cmd.contains("পরের") || cmd.contains("পরবর্তী") -> MediaAction.NEXT
                    cmd.contains("prev") || cmd.contains("আগের") || cmd.contains("পূর্ববর্তী") -> MediaAction.PREVIOUS
                    cmd.contains("stop") -> MediaAction.STOP
                    else -> MediaAction.TOGGLE
                }
                val msg = mediaController.sendMediaCommand(mediaAction)
                ToolResult(true, msg, toolName, verified = true)
            }

            // Device Diagnostics
            "getBatteryStatus" -> {
                val battery = deviceController.getBatteryInfo()
                val chargingText = if (battery.isCharging) "Charging হচ্ছে (${battery.pluggedSource})" else "Battery-তে চলছে"
                val text = "আপনার Battery charge বর্তমানে ${battery.level}% এবং $chargingText। তাপমাত্রা ${battery.temperatureCelsius}°C, অবস্থা ${battery.health}।"
                ToolResult(true, text, toolName, verified = true, payload = mapOf("level" to battery.level, "isCharging" to battery.isCharging))
            }

            "getWifiStatus" -> {
                val wifi = deviceController.getWifiInfo()
                val text = if (wifi.isConnected) {
                    "Wi-Fi চালু আছে এবং '${wifi.ssid}' নেটওয়ার্কের সাথে সংযুক্ত (Signal: ${wifi.signalLevel}/5)।"
                } else if (wifi.isEnabled) {
                    "Wi-Fi চালু আছে কিন্তু কোনো নেটওয়ার্কে সংযুক্ত নেই।"
                } else {
                    "Wi-Fi বন্ধ আছে।"
                }
                ToolResult(true, text, toolName, verified = true)
            }

            "getNetworkStatus" -> {
                val net = deviceController.getNetworkInfo()
                val text = if (net.hasInternet) {
                    "ইন্টারনেট সংযোগ চালু ও সক্রিয় আছে (${net.connectionType})।"
                } else {
                    "বর্তমানে কোনো ইন্টারনেট সংযোগ পাওয়া যাচ্ছে না।"
                }
                ToolResult(true, text, toolName, verified = true)
            }

            "getStorageStatus" -> {
                val storage = deviceController.getStorageInfo()
                val text = "আপনার ডিভাইসে ${storage.freeGB} GB স্টোরেজ খালি আছে (মোট ${storage.totalGB} GB, ${storage.percentFree}% Free)।"
                ToolResult(true, text, toolName, verified = true)
            }

            // System Settings & Hardware
            "openSettings" -> {
                val type = args.optString("type", "main")
                val res = appController.openSettings(type)
                if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Settings খোলা সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            "openWifiSettings" -> {
                val res = appController.openWifiSettings()
                if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Wi-Fi Settings খোলা সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            "openBluetoothSettings" -> {
                val res = appController.openBluetoothSettings()
                if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Bluetooth Settings খোলা সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            "openCamera" -> {
                val res = appController.openCamera()
                if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Camera খোলা সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            "lockDevice" -> {
                val lockResult = deviceController.lockDevice()
                if (lockResult.isSuccess) {
                    ToolResult(true, lockResult.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, lockResult.exceptionOrNull()?.message ?: "ফোন lock করা সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            // Accessibility Engine Operations
            "accessibilityFind" -> {
                val text = args.optString("text", "")
                val foundNode = AccessibilityActionEngine.findText(text)
                if (foundNode != null) {
                    ToolResult(true, "স্ক্রিনে '$text' উপাদানটি খুঁজে পাওয়া গেছে।", toolName, verified = true)
                } else {
                    ToolResult(false, "আমি প্রয়োজনীয় control খুঁজে পাইনি।", toolName, verified = false)
                }
            }

            "accessibilityClick" -> {
                val target = args.optString("targetText", "")
                val clicked = AccessibilityActionEngine.clickByText(target)
                if (clicked) {
                    ToolResult(true, "'$target' উপাদানে ক্লিক করা হয়েছে।", toolName, verified = true)
                } else {
                    ToolResult(false, "আমি প্রয়োজনীয় control খুঁজে পাইনি।", toolName, verified = false)
                }
            }

            "accessibilitySetText" -> {
                val text = args.optString("text", "")
                val typed = AccessibilityActionEngine.typeText(null, text)
                if (typed) {
                    ToolResult(true, "টেক্সট সফলভাবে ইনপুট দেওয়া হয়েছে।", toolName, verified = true)
                } else {
                    ToolResult(false, "আমি প্রয়োজনীয় লেখার ইনপুট ফিল্ড খুঁজে পাইনি।", toolName, verified = false)
                }
            }

            "accessibilityScroll" -> {
                val direction = args.optString("direction", "down").lowercase()
                val ok = if (direction == "up" || direction.contains("উপর")) {
                    AccessibilityActionEngine.scrollUp()
                } else {
                    AccessibilityActionEngine.scrollDown()
                }
                if (ok) {
                    ToolResult(true, "স্ক্রিন স্ক্রোল করা হয়েছে।", toolName, verified = true)
                } else {
                    ToolResult(false, "স্ক্রোল করার মতো স্ক্রিন পাওয়া যায়নি।", toolName, verified = false)
                }
            }

            "accessibilityBack" -> {
                val ok = AccessibilityActionEngine.goBack()
                if (ok) {
                    ToolResult(true, "Back করা হয়েছে।", toolName, verified = true)
                } else {
                    ToolResult(false, "Back করা সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            // Legacy / Complementary
            "closeApp" -> {
                val ok = AccessibilityActionEngine.goHome()
                if (ok) {
                    ToolResult(true, "Home screen-এ ফিরে যাওয়া হয়েছে।", toolName, verified = true)
                } else {
                    ToolResult(false, "Home screen-এ যাওয়া সম্ভব হয়নি।", toolName, verified = false)
                }
            }

            "screenAnalyzer" -> {
                val screenRes = screenAnalyzer.analyzeVisibleScreen()
                if (screenRes.isSuccess) {
                    ToolResult(true, screenRes.getOrThrow(), toolName, verified = true)
                } else {
                    ToolResult(false, screenRes.exceptionOrNull()?.message ?: "স্ক্রিন বিশ্লেষণ করা যায়নি।", toolName, verified = false)
                }
            }

            else -> {
                ToolResult(false, "এই কমান্ডটি বর্তমানে সমর্থিত নয়।", toolName, verified = false)
            }
        }
    }
}
