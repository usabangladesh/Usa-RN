package com.example.apps

import android.content.Context
import com.example.accessibility.JarvisAccessibilityService
import com.example.assistant.ActionQueue
import kotlinx.coroutines.delay

/**
 * UniversalAppControlEngine (Section 3).
 * Central Brain orchestrating:
 * Bangla Command -> App Detection -> Context Tracking -> Controller Routing ->
 * Accessibility UI Execution -> ActionQueue -> Verification -> Voice Feedback -> Listen Again.
 */
class UniversalAppControlEngine(
    private val context: Context,
    val appController: AppController,
    val whatsAppController: WhatsAppController,
    val youTubeController: YouTubeController,
    val facebookController: FacebookController,
    val tiktokController: TikTokController,
    val genericAppController: GenericAppController
) {

    private val actionQueue = ActionQueue()

    private fun getService(): JarvisAccessibilityService? = JarvisAccessibilityService.instance

    fun getCurrentForegroundPackage(): String? {
        return getService()?.getActivePackage()
    }

    /**
     * Executes single or multi-step command sequences
     */
    suspend fun processCommand(
        command: String,
        onFeedback: (String) -> Unit,
        onRequireConfirmation: (String, suspend () -> AppActionResult) -> Unit,
        onFinished: (Boolean, String) -> Unit
    ) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) {
            onFinished(false, "বস, আপনি কী করতে চান তা স্পষ্ট করে বলুন।")
            return
        }

        // Multi-step detection: split on compound markers (এবং, তারপর, then, and, comma)
        val steps = splitCompoundCommands(trimmed)
        actionQueue.clear()

        for (stepText in steps) {
            enqueueStep(stepText)
        }

        var lastMessage = "বস, কাজটি সম্পন্ন হয়েছে।"
        val allSucceeded = actionQueue.executeAll(
            onStepRunning = { desc ->
                // Voice immediate feedback: No silent actions!
                onFeedback(desc)
            },
            onStepCompleted = { desc, result ->
                lastMessage = result.message
            },
            onStepFailed = { desc, error ->
                lastMessage = error
                onFinished(false, error)
            },
            onRequiresConfirmation = { result ->
                if (result.pendingAction != null) {
                    onRequireConfirmation(result.message, result.pendingAction)
                }
            }
        )

        if (allSucceeded) {
            onFinished(true, lastMessage)
        }
    }

    private fun splitCompoundCommands(command: String): List<String> {
        val lower = command.lowercase()
        // If command contains explicit connectors
        val regex = Regex("\\s+(এবং|তারপর|then|and)\\s+|\\s*,\\s*", RegexOption.IGNORE_CASE)
        val parts = command.split(regex).map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts else listOf(command)
    }

    private fun enqueueStep(step: String) {
        val lower = step.lowercase()

        // 1. WhatsApp Chat Open: "Rahim-এর chat খোলো" / "Rahim er chat kholo"
        if ((lower.contains("chat খোলো") || lower.contains("চ্যাট খোলো") || lower.contains("chat kholo") || lower.contains("মেসেজ খোলো")) &&
            !lower.contains("type") && !lower.contains("টাইপ")
        ) {
            val recipient = extractContactName(step)
            actionQueue.enqueue("বস, $recipient-এর chat খুঁজছি।") {
                whatsAppController.openChat(recipient)
            }
            return
        }

        // 2. Typing: "টাইপ করো: [text]" / "Type koro [text]" / "এখানে টাইপ করো [text]"
        if (lower.contains("টাইপ করো") || lower.contains("type koro") || lower.contains("লেখ") || lower.contains("লিখে দাও") || lower.contains("টাইপ কর")) {
            val textToType = extractTextToType(step)
            actionQueue.enqueue("বস, এখানে টাইপ করছি।") {
                val currentPkg = getCurrentForegroundPackage() ?: ""
                if (currentPkg.contains("whatsapp", ignoreCase = true)) {
                    whatsAppController.typeMessage(textToType)
                } else {
                    genericAppController.universalType(textToType)
                }
            }
            return
        }

        // 3. Search: "Ronaldo search করো" / "Search koro Ronaldo" / "Search অপশন খোলো" / "Ronaldo লিখে search করো"
        if (lower.contains("search করো") || lower.contains("সার্চ করো") || lower.contains("search koro") ||
            lower.contains("অনুসন্ধান করো") || lower.contains("search অপশন খোলো") || lower.contains("খুঁজে বের করো")
        ) {
            val query = extractSearchQuery(step)
            actionQueue.enqueue("বস, অনুসন্ধান করছি।") {
                val currentPkg = getCurrentForegroundPackage() ?: ""
                when {
                    currentPkg.contains("youtube", ignoreCase = true) || lower.contains("youtube") -> {
                        youTubeController.search(query)
                    }
                    currentPkg.contains("facebook", ignoreCase = true) || lower.contains("facebook") -> {
                        facebookController.search(query)
                    }
                    currentPkg.contains("tiktok", ignoreCase = true) || lower.contains("tiktok") -> {
                        tiktokController.search(query)
                    }
                    else -> {
                        genericAppController.universalSearch(query)
                    }
                }
            }
            return
        }

        // 4. Scroll: "নিচে যাও" / "উপরে যাও" / "Scroll down" / "Scroll up"
        if (lower.contains("নিচে যাও") || lower.contains("আরও নিচে") || lower.contains("scroll down") || lower.contains("down")) {
            actionQueue.enqueue("বস, নিচে যাচ্ছি।") {
                genericAppController.universalScroll("down")
            }
            return
        }
        if (lower.contains("উপরে যাও") || lower.contains("আরও উপরে") || lower.contains("scroll up") || lower.contains("up")) {
            actionQueue.enqueue("বস, উপরে যাচ্ছি।") {
                genericAppController.universalScroll("up")
            }
            return
        }

        // 5. Back: "Back করো" / "আগের পেজে যাও" / "Go back"
        if (lower.contains("back করো") || lower.contains("আগের পেজে যাও") || lower.contains("পিছনে যাও") || lower.contains("go back") || lower == "back") {
            actionQueue.enqueue("বস, ব্যাক করছি।") {
                genericAppController.universalBack()
            }
            return
        }

        // 6. Click / Open Element: "এটা open করো" / "এইটা click করো" / "[Text] click করো"
        if (lower.contains("click করো") || lower.contains("ক্লিক করো") || lower.contains("ক্লিক কর") || lower.contains("open করো") || lower.contains("ওপেন করো")) {
            val target = extractClickTarget(step)
            actionQueue.enqueue("বস, \"$target\"-এ ক্লিক করছি।") {
                genericAppController.universalClick(target)
            }
            return
        }

        // 7. App Open: WhatsApp, YouTube, Facebook, etc.
        if (lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") || lower.contains("হোয়াটসঅ্যাপ")) {
            actionQueue.enqueue("বস, WhatsApp খুলছি।") {
                whatsAppController.openWhatsApp()
            }
            return
        }
        if (lower.contains("youtube") || lower.contains("ইউটিউব")) {
            actionQueue.enqueue("বস, YouTube খুলছি।") {
                youTubeController.openYouTube()
            }
            return
        }
        if (lower.contains("facebook") || lower.contains("ফেসবুক")) {
            actionQueue.enqueue("বস, Facebook খুলছি।") {
                facebookController.openFacebook()
            }
            return
        }
        if (lower.contains("tiktok") || lower.contains("টিকটক")) {
            actionQueue.enqueue("বস, TikTok খুলছি।") {
                tiktokController.openTikTok()
            }
            return
        }

        // 8. Generic App Open
        if (lower.contains("খোলো") || lower.contains("খুলে দাও") || lower.contains("चालू करो") || lower.contains("open ") || lower.contains("kholo")) {
            val appName = step
                .replace(Regex("(খোলো|খুলে দাও|চালু করো|open|kholo)", RegexOption.IGNORE_CASE), "")
                .trim()
            actionQueue.enqueue("বস, $appName খুলছি।") {
                genericAppController.openApp(appName)
            }
            return
        }

        // 9. Default fallback: Try as generic click or search
        actionQueue.enqueue("বস, ব্যবস্থা করছি।") {
            genericAppController.universalClick(step)
        }
    }

    private fun extractContactName(text: String): String {
        return text
            .replace(Regex("(-এর chat খোলো|-এর চ্যাট খোলো|er chat kholo|এর chat খোলো|এর চ্যাট খোলো|chat খোলো|চ্যাট খোলো|chat kholo)", RegexOption.IGNORE_CASE), "")
            .replace("WhatsApp", "", ignoreCase = true)
            .replace("খোলো", "")
            .replace("kholo", "", ignoreCase = true)
            .trim()
    }

    private fun extractTextToType(text: String): String {
        val colonSplit = text.split(":")
        if (colonSplit.size > 1) {
            return colonSplit[1].trim()
        }
        return text
            .replace(Regex("(টাইপ করো|type koro|এখানে টাইপ করো|লেখ|লিখে দাও|বলো)", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun extractSearchQuery(text: String): String {
        return text
            .replace(Regex("(search অপশন খোলো|সার্চ অপশন খোলো)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(লিখে search করো|search করো|সার্চ করো|search koro|অনুসন্ধান করো|খুঁজে বের করো)", RegexOption.IGNORE_CASE), "")
            .replace("YouTube", "", ignoreCase = true)
            .replace("ইউটিউব", "")
            .replace("Facebook", "", ignoreCase = true)
            .replace("TikTok", "", ignoreCase = true)
            .trim()
    }

    private fun extractClickTarget(text: String): String {
        return text
            .replace(Regex("(এটা open করো|এইটা click করো|click করো|ক্লিক করো|open করো|ওপেন করো)", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun stop() {
        actionQueue.clear()
    }
}
