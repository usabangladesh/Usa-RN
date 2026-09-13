package com.example.intent

import org.json.JSONObject

sealed class ParsedIntent {
    data class DirectTool(val toolName: String, val args: JSONObject) : ParsedIntent()
    data class DirectSpeech(val message: String) : ParsedIntent()
    object EmergencyStop : ParsedIntent()
    object UserConfirmed : ParsedIntent()
    object UserCancelled : ParsedIntent()
    data class GeneralGemini(val userText: String) : ParsedIntent()
}

object IntentRouter {

    fun parseUserCommand(input: String): ParsedIntent {
        val text = input.trim()
        val lower = text.lowercase()

        // 1. Emergency stop check (Section 20 & 34)
        val stopKeywords = listOf("থামো", "থামাও", "চুপ করো", "stop", "halt", "ruko", "band karo")
        if (stopKeywords.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") || lower.contains("রশিদ থামো") || lower.contains("rashed থামো") }) {
            return ParsedIntent.EmergencyStop
        }

        // 2. Confirmation check (Section 9)
        val confirmKeywords = listOf("হ্যাঁ", "হ্যা", "পাঠাও", "করো", "send", "yes", "yep", "sure", "ok", "হুম", "ঠিক আছে", "भेजो", "हाँ")
        if (confirmKeywords.any { lower == it || lower.startsWith("$it ") }) {
            return ParsedIntent.UserConfirmed
        }

        // 3. Cancellation check
        val cancelKeywords = listOf("না", "বাতিল", "করো না", "cancel", "no", "nope", "don't", "dont", "mat bhejo", "नहीं")
        if (cancelKeywords.any { lower == it || lower.startsWith("$it ") }) {
            return ParsedIntent.UserCancelled
        }

        // 4. Back navigation (Section 16, Test 3)
        if (lower.contains("back") || lower.contains("আগের পাতা") || lower.contains("পিছনে যাও") || lower.contains("পিছে যাও") || lower.contains("ফিরে যাও")) {
            return ParsedIntent.DirectTool("accessibilityBack", JSONObject())
        }

        // 5. Scroll navigation (Section 16)
        if (lower.contains("scroll") || lower.contains("স্ক্রোল")) {
            val dir = if (lower.contains("উপরে") || lower.contains("up")) "up" else "down"
            return ParsedIntent.DirectTool("accessibilityScroll", JSONObject().put("direction", dir))
        }

        // 6. YouTube Specific Automation (Section 6 & 7, Tests 1 & 2)
        if (lower.contains("youtube") || lower.contains("ইউটিউব")) {
            if (lower.contains("search") || lower.contains("সার্চ") || lower.contains("খুঁজ") || lower.contains("খোজ")) {
                // Extract query: e.g. "YouTube-এ Ronaldo search করো"
                val cleanQuery = extractYouTubeQuery(text)
                return ParsedIntent.DirectTool("searchYouTube", JSONObject().put("query", cleanQuery))
            }
            if (lower.contains("ভিডিও") && (lower.contains("চালাও") || lower.contains("play"))) {
                val videoTitle = extractYouTubeQuery(text)
                return ParsedIntent.DirectTool("playYouTubeVideo", JSONObject().put("videoTitle", videoTitle))
            }
            if (lower.contains("খোল") || lower.contains("open") || lower.contains("চালু")) {
                return ParsedIntent.DirectTool("openApp", JSONObject().put("appName", "YouTube"))
            }
        }

        // Standalone video play: e.g. "Ronaldo-এর ভিডিওটা চালাও"
        if (lower.contains("ভিডিও") && (lower.contains("চালাও") || lower.contains("প্লে") || lower.contains("play"))) {
            val videoTitle = text.replace("এর ভিডিওটা চালাও", "")
                .replace("এর ভিডিও চালাও", "")
                .replace("ভিডিও চালাও", "")
                .replace("ভিডিওটা চালাও", "")
                .replace("play video", "")
                .replace("-এর", "")
                .trim()
            return ParsedIntent.DirectTool("playYouTubeVideo", JSONObject().put("videoTitle", videoTitle))
        }

        // 7. WhatsApp Contact & Message Automation (Section 8, Tests 4 & 5)
        if (lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") || lower.contains("হোয়াটসঅ্যাপ")) {
            if (lower.contains("chat") || lower.contains("চ্যাট") || lower.contains("খোলো") || lower.contains("খোল")) {
                val contactName = extractContactName(text)
                if (contactName.isNotBlank()) {
                    return ParsedIntent.DirectTool("openWhatsAppConversation", JSONObject().put("contactName", contactName))
                }
            }
        }

        // WhatsApp message command: e.g. "Rahim-কে লিখো আমি পরে আসব" or "Rahim-কে বলো আমি আজ আসতে পারব না"
        val messageMatch = detectWhatsAppMessageIntent(lower, text)
        if (messageMatch != null) {
            return ParsedIntent.DirectTool("prepareMessage", messageMatch)
        }

        // 8. Battery queries (Section 15)
        if (lower.contains("battery") || lower.contains("ব্যাটারি") || lower.contains("চার্জ কত") || lower.contains("charging হচ্ছে")) {
            return ParsedIntent.DirectTool("getBatteryStatus", JSONObject())
        }

        // 9. Wi-Fi queries & Settings
        if (lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("ওয়াইফাই") || lower.contains("ওয়াইফাই")) {
            if (lower.contains("setting") || lower.contains("সেটিংস")) {
                return ParsedIntent.DirectTool("openWifiSettings", JSONObject())
            }
            return ParsedIntent.DirectTool("getWifiStatus", JSONObject())
        }

        // 10. Internet connectivity
        if (lower.contains("internet") || lower.contains("ইন্টারনেট") || lower.contains("নেট আছে") || lower.contains("নেটওয়ার্ক")) {
            return ParsedIntent.DirectTool("getNetworkStatus", JSONObject())
        }

        // 11. Storage queries (Section 15)
        if (lower.contains("storage") || lower.contains("স্টোরেজ") || lower.contains("মেমরি কত খালি") || lower.contains("storage কত খালি")) {
            return ParsedIntent.DirectTool("getStorageStatus", JSONObject())
        }

        // 12. Device lock
        if (lower.contains("lock") || lower.contains("লক") || lower.contains("স্ক্রিন অফ") || lower.contains("screen off")) {
            return ParsedIntent.DirectTool("lockDevice", JSONObject())
        }

        // 13. Volume control (Section 13, Test 6)
        if (lower.contains("volume") || lower.contains("ভলিউম") || lower.contains("আওয়াজ") || lower.contains("সাউন্ড")) {
            return when {
                lower.contains("কমা") || lower.contains("down") || lower.contains("low") || lower.contains("কম") -> {
                    ParsedIntent.DirectTool("volumeControl", JSONObject().put("action", "down"))
                }
                lower.contains("বাড়া") || lower.contains("up") || lower.contains("high") || lower.contains("বেশি") -> {
                    ParsedIntent.DirectTool("volumeControl", JSONObject().put("action", "up"))
                }
                lower.contains("mute") || lower.contains("মিউট") || lower.contains("সাইলেন্ট") -> {
                    ParsedIntent.DirectTool("volumeControl", JSONObject().put("action", "mute"))
                }
                lower.contains("unmute") || lower.contains("আনমিউট") -> {
                    ParsedIntent.DirectTool("volumeControl", JSONObject().put("action", "unmute"))
                }
                else -> ParsedIntent.GeneralGemini(text)
            }
        }

        // 14. Media control (Section 14, Test 7)
        if (lower.contains("গান pause") || lower.contains("গান থামাও") || lower.contains("মিউজিক pause") || lower.contains("গান বন্ধ")) {
            return ParsedIntent.DirectTool("mediaControl", JSONObject().put("command", "pause"))
        }
        if (lower.contains("next song") || lower.contains("পরের গান") || lower.contains("next track")) {
            return ParsedIntent.DirectTool("mediaControl", JSONObject().put("command", "next"))
        }
        if (lower.contains("previous song") || lower.contains("আগের গান")) {
            return ParsedIntent.DirectTool("mediaControl", JSONObject().put("command", "previous"))
        }
        if (lower.contains("গান চালাও") || lower.contains("গান আবার চালাও") || lower.contains("play music") || lower.contains("গান বাজা")) {
            return ParsedIntent.DirectTool("mediaControl", JSONObject().put("command", "play"))
        }

        // 15. Camera
        if (lower.contains("camera") || lower.contains("ক্যামেরা") || lower.contains("ছবি তোল") || lower.contains("ক্যামেরা খোলো")) {
            return ParsedIntent.DirectTool("openCamera", JSONObject())
        }

        // 16. Bluetooth settings
        if (lower.contains("bluetooth") || lower.contains("ব্লুটুথ")) {
            if (lower.contains("setting") || lower.contains("সেটিংস")) {
                return ParsedIntent.DirectTool("openBluetoothSettings", JSONObject())
            }
        }

        // 17. App launches (Generic)
        if (lower.contains("খুলে দাও") || lower.contains("খোলো") || lower.contains("চালু করো") || lower.contains("open ")) {
            val appCandidate = text
                .replace("খুলে দাও", "")
                .replace("খোলো", "")
                .replace("চালু করো", "")
                .replace("open ", "", ignoreCase = true)
                .trim()
            if (appCandidate.isNotBlank() && appCandidate.length < 25) {
                return ParsedIntent.DirectTool("openApp", JSONObject().put("appName", appCandidate))
            }
        }

        // 18. Quick Conversational Bangla Replies (Instant voice response)
        if (lower.contains("কেমন আছো") || lower.contains("কেমন আছেন") || lower.contains("how are you")) {
            return ParsedIntent.DirectSpeech("আমি ভালো আছি! আপনার ফোনে যেকোনো কাজের জন্য বলুন, আমি করে দিচ্ছি।")
        }
        if (lower.contains("তোমার নাম কি") || lower.contains("তোমার নাম কী") || lower.contains("who are you") || lower.contains("তুমি কে")) {
            return ParsedIntent.DirectSpeech("আমি Rashed AI, আপনার বাস্তব Android Voice Assistant। বলুন, কীভাবে সাহায্য করতে পারি?")
        }
        if (lower.contains("ধন্যবাদ") || lower.contains("থ্যাংক") || lower.contains("thank you") || lower.contains("thanks")) {
            return ParsedIntent.DirectSpeech("আপনাকে অনেক ধন্যবাদ! যেকোনো প্রয়োজনে আমাকে ডাকবেন।")
        }
        if (lower.contains("কী করতে পারো") || lower.contains("কি কি করতে পারো") || lower.contains("কি করতে পারো") || lower.contains("what can you do")) {
            return ParsedIntent.DirectSpeech("আমি অ্যাপ খোলা, ইউটিউবে ভিডিও চালানো, হোয়াটসঅ্যাপে মেসেজ পাঠানো, ভলিউম ও মিডিয়া নিয়ন্ত্রণ এবং ব্যাটারি ও ফোন লক করা সহ নানা কাজ করতে পারি।")
        }
        if (lower == "হাই" || lower == "হ্যালো" || lower == "হ্যালো রাশেদ" || lower == "হাই রাশেদ" || lower == "রাশেদ" || lower == "rashed" || lower == "hello" || lower == "hi") {
            return ParsedIntent.DirectSpeech("হ্যালো! আমি শুনছি, বলুন কী সাহায্য করতে পারি?")
        }

        // Default: Forward to Gemini reasoning & dynamic tool execution
        return ParsedIntent.GeneralGemini(text)
    }

    private fun extractYouTubeQuery(text: String): String {
        return text
            .replace("Rashed,", "", ignoreCase = true)
            .replace("Rashed", "", ignoreCase = true)
            .replace("YouTube-এ", "", ignoreCase = true)
            .replace("YouTube এ", "", ignoreCase = true)
            .replace("YouTube", "", ignoreCase = true)
            .replace("ইউটিউব-এ", "")
            .replace("ইউটিউবে", "")
            .replace("ইউটিউব", "")
            .replace("search করো", "", ignoreCase = true)
            .replace("search", "", ignoreCase = true)
            .replace("সার্চ করো", "")
            .replace("খুঁজে বের করো", "")
            .replace("খোঁজ", "")
            .replace("খুলো", "")
            .replace("খুলে", "")
            .trim()
    }

    private fun extractContactName(text: String): String {
        val words = text.split(" ")
        for (w in words) {
            val clean = w.replace("-এর", "").replace("এর", "").replace("-কে", "").replace("কে", "")
            if (w.contains("-এর") || w.contains("এর")) {
                return clean
            }
        }
        return ""
    }

    private fun detectWhatsAppMessageIntent(lower: String, original: String): JSONObject? {
        val isMessageIntent = lower.contains("message") || lower.contains("মেসেজ") ||
                lower.contains("বলো") || lower.contains("লিখো") || lower.contains("পাঠাও") || lower.contains("bolo")

        if (!isMessageIntent) return null

        val words = original.split(" ")
        var recipient = ""
        var message = ""

        for ((index, word) in words.withIndex()) {
            val clean = word.replace(",", "").replace("-কে", "").replace("কে", "")
            if (word.contains("-কে") || (word.endsWith("কে") && index > 0)) {
                recipient = clean
                if (index + 1 < words.size) {
                    message = words.subList(index + 1, words.size).joinToString(" ")
                        .replace("বলো", "")
                        .replace("লিখো", "")
                        .replace("message দাও", "")
                        .replace("মেসেজ দাও", "")
                        .replace("পাঠাও", "")
                        .trim()
                }
                break
            }
        }

        if (recipient.isNotBlank() && message.isNotBlank()) {
            return JSONObject().apply {
                put("recipient", recipient)
                put("messageText", message)
            }
        }

        return null
    }
}
