package com.example.tools

import android.content.Context
import com.example.accessibility.RashedAccessibilityService
import com.example.apps.AppController
import com.example.device.DeviceController
import com.example.media.MediaAction
import com.example.media.MediaController
import com.example.media.VolumeDirection
import com.example.notifications.RashedNotificationListenerService
import com.example.screen.ScreenAnalyzer
import com.example.security.ConfirmationManager
import com.example.security.PendingConfirmation
import com.example.security.SecurityManager
import org.json.JSONObject

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
        // Security check
        if (securityManager.isRestricted(toolName, args.toString())) {
            return ToolResult(
                success = false,
                toolName = toolName,
                message = "নিরাপত্তা ও গোপনীয়তা বিধির কারণে এই কাজটি করা সম্ভব নয় (Action restricted by security policy)."
            )
        }

        return when (toolName) {
            "openApp" -> {
                val appName = args.optString("appName", "")
                if (appName.isBlank()) {
                    ToolResult(false, "কোন অ্যাপ খুলতে হবে তা নির্দিষ্ট করুন।", toolName)
                } else {
                    val result = appController.openApp(appName)
                    if (result.isSuccess) {
                        ToolResult(true, result.getOrThrow(), toolName)
                    } else {
                        ToolResult(false, result.exceptionOrNull()?.message ?: "অ্যাপ খুলতে ব্যর্থ হয়েছে।", toolName)
                    }
                }
            }

            "closeApp" -> {
                val service = RashedAccessibilityService.instance
                val ok = service?.goHome() ?: false
                if (ok) {
                    ToolResult(true, "Home screen-এ ফিরে যাওয়া হয়েছে।", toolName)
                } else {
                    // Fallback to launcher intent
                    val startMain = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_HOME)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(startMain)
                        ToolResult(true, "Home screen-এ ফিরে যাওয়া হয়েছে।", toolName)
                    } catch (e: Exception) {
                        ToolResult(false, "Home screen-এ যাওয়া সম্ভব হয়নি।", toolName)
                    }
                }
            }

            "openWebsite" -> {
                val query = args.optString("urlOrQuery", "")
                val result = appController.openWebsite(query)
                if (result.isSuccess) {
                    ToolResult(true, result.getOrThrow(), toolName)
                } else {
                    ToolResult(false, result.exceptionOrNull()?.message ?: "ব্রাউজার খুলতে সমস্যা হয়েছে।", toolName)
                }
            }

            "getBatteryStatus" -> {
                val battery = deviceController.getBatteryInfo()
                val chargingText = if (battery.isCharging) "Charging হচ্ছে (${battery.pluggedSource})" else "Battery-তে চলছে"
                val text = "আপনার Battery charge বর্তমানে ${battery.level}% এবং $chargingText। তাপমাত্রা ${battery.temperatureCelsius}°C, অবস্থা ${battery.health}।"
                ToolResult(true, text, toolName, payload = mapOf("level" to battery.level, "isCharging" to battery.isCharging))
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
                ToolResult(true, text, toolName)
            }

            "getNetworkStatus" -> {
                val net = deviceController.getNetworkInfo()
                val text = if (net.hasInternet) {
                    "ইন্টারনেট সংযোগ চালু ও সক্রিয় আছে (${net.connectionType})।"
                } else {
                    "বর্তমানে ইন্টারনেট সংযোগ পাওয়া যাচ্ছে না।"
                }
                ToolResult(true, text, toolName)
            }

            "getDeviceStatus" -> {
                val overview = deviceController.getDeviceOverview()
                val storage = deviceController.getStorageInfo()
                val text = "$overview | Storage: ${storage.freeGB} GB খালি আছে (মোট ${storage.totalGB} GB, ${storage.percentFree}% Free)।"
                ToolResult(true, text, toolName)
            }

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
                ToolResult(true, msg, toolName)
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
                ToolResult(true, msg, toolName)
            }

            "lockDevice" -> {
                val lockResult = deviceController.lockDevice()
                if (lockResult.isSuccess) {
                    ToolResult(true, lockResult.getOrThrow(), toolName)
                } else {
                    ToolResult(false, lockResult.exceptionOrNull()?.message ?: "ফোন lock করা সম্ভব হয়নি।", toolName)
                }
            }

            "notificationReader" -> {
                val notifications = RashedNotificationListenerService.recentNotifications
                if (notifications.isEmpty()) {
                    ToolResult(true, "বর্তমানে কোনো নতুন নোটিফিকেশন নেই।", toolName)
                } else {
                    val limit = args.optInt("limit", 3).coerceIn(1, 5)
                    val top = notifications.take(limit)
                    val sb = StringBuilder("আপনার সাম্প্রতিক নোটিফিকেশনগুলো হলো:\n")
                    for ((index, n) in top.withIndex()) {
                        sb.append("${index + 1}. ${n.appName} থেকে ${n.title}: ${n.text}\n")
                    }
                    ToolResult(true, sb.toString().trim(), toolName)
                }
            }

            "screenAnalyzer" -> {
                val screenRes = screenAnalyzer.analyzeVisibleScreen()
                if (screenRes.isSuccess) {
                    ToolResult(true, screenRes.getOrThrow(), toolName)
                } else {
                    ToolResult(false, screenRes.exceptionOrNull()?.message ?: "স্ক্রিন বিশ্লেষণ করা যায়নি।", toolName)
                }
            }

            "findContact" -> {
                val name = args.optString("name", "")
                val contact = appController.findContactByName(name)
                if (contact != null) {
                    val phoneStr = contact.phoneNumber ?: "নম্বর পাওয়া যায়নি"
                    ToolResult(true, "${contact.displayName} এর নম্বর পাওয়া গেছে: $phoneStr", toolName)
                } else {
                    ToolResult(false, "'$name' নামে কোনো যোগাযোগ নম্বর পাওয়া যায়নি।", toolName)
                }
            }

            "openConversation" -> {
                val contactName = args.optString("contactName", "")
                val res = appController.openWhatsAppConversation(contactName, null)
                if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow(), toolName)
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "কথোপকথন খোলা সম্ভব হয়নি।", toolName)
                }
            }

            "prepareMessage" -> {
                val recipient = args.optString("recipient", "")
                val messageText = args.optString("messageText", "")
                if (recipient.isBlank() || messageText.isBlank()) {
                    return ToolResult(false, "প্রাপক বা মেসেজের বিষয়বস্তু অনুপস্থিত।", toolName)
                }

                // Strictly follow prompt Section 4:
                // Ask confirmation before sending: "Rahim-এর জন্য এই মেসেজটি প্রস্তুত করেছি। Send করব?"
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
                    requiresConfirmation = true,
                    pendingActionPrompt = promptText
                )
            }

            else -> {
                ToolResult(false, "এই কমান্ডটি বর্তমানে সমর্থিত নয়।", toolName)
            }
        }
    }
}
