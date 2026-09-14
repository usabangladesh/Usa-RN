package com.example.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.accessibility.JarvisAccessibilityService
import kotlinx.coroutines.delay
import java.net.URLEncoder

/**
 * Dedicated Controller for WhatsApp (Section 12).
 * Handles deep automated chat navigation, contact search, typing, confirmation, and sending.
 */
class WhatsAppController(
    private val context: Context,
    private val appController: AppController
) {

    private fun getService(): JarvisAccessibilityService? = JarvisAccessibilityService.instance

    suspend fun openWhatsApp(): AppActionResult {
        val res = appController.openApp("whatsapp")
        if (res.isFailure) {
            return AppActionResult(
                success = false,
                message = "বস, WhatsApp ফোনে ইনস্টল করা নেই বা খোলা সম্ভব হয়নি।",
                verified = false
            )
        }

        val service = getService()
        val ready = service?.waitForPackage("com.whatsapp", 3000) ?: true
        return if (ready) {
            AppActionResult(
                success = true,
                message = "বস, WhatsApp খুলছি।",
                verified = true
            )
        } else {
            AppActionResult(
                success = false,
                message = "বস, WhatsApp প্রস্তুত হতে সময় লাগছে।",
                verified = false
            )
        }
    }

    suspend fun openChat(recipient: String): AppActionResult {
        val cleanRecipient = recipient.trim()
        if (cleanRecipient.isBlank()) {
            return AppActionResult(false, "বস, কার চ্যাট খুলতে চান তার নাম বলুন।")
        }

        val service = getService()
        if (service == null) {
            // Fallback via official WhatsApp URI intent
            val res = appController.openWhatsAppConversation(cleanRecipient, null)
            return if (res.isSuccess) {
                AppActionResult(true, "বস, $cleanRecipient-এর চ্যাট খুলছি।", verified = true)
            } else {
                AppActionResult(false, "বস, Accessibility চালু না থাকায় সরাসরি $cleanRecipient-এর চ্যাট খোলা যাচ্ছে না।")
            }
        }

        // Wait if WhatsApp is not in foreground yet
        val currentPkg = service.getActivePackage()
        if (currentPkg?.contains("whatsapp", ignoreCase = true) != true) {
            appController.openApp("whatsapp")
            service.waitForPackage("com.whatsapp", 3000)
            delay(500)
        }

        // 1. Check if recipient's name is already visible in recent chats
        val existingChat = service.findClickableNode(cleanRecipient)
        if (existingChat != null) {
            service.clickNode(existingChat)
            delay(600)
            return AppActionResult(true, "বস, $cleanRecipient-এর chat খুলছি।", verified = true)
        }

        // 2. Click WhatsApp search button
        val searchNode = service.findSearchNode()
        if (searchNode != null) {
            service.clickNode(searchNode)
            delay(400)
            service.typeText(null, cleanRecipient)
            delay(600)

            // Inspect search results
            val matchingItems = service.extractScreenHierarchyText().filter {
                it.contains(cleanRecipient, ignoreCase = true) && !it.contains("search", ignoreCase = true)
            }

            if (matchingItems.size > 1) {
                return AppActionResult(
                    success = false,
                    message = "বস, একই নামে একাধিকজন পাওয়া গেছে। কোন $cleanRecipient-কে চাই?",
                    verified = false
                )
            }

            val chatResultNode = service.findClickableNode(cleanRecipient)
            if (chatResultNode != null) {
                service.clickNode(chatResultNode)
                delay(600)
                return AppActionResult(true, "বস, $cleanRecipient-এর chat খুলছি।", verified = true)
            }
        }

        // 3. Fallback via contact query & WhatsApp Intent
        val fallbackIntentRes = appController.openWhatsAppConversation(cleanRecipient, null)
        return if (fallbackIntentRes.isSuccess) {
            AppActionResult(true, "বস, $cleanRecipient-এর chat খুলছি।", verified = true)
        } else {
            AppActionResult(
                false,
                "বস, $cleanRecipient-কে WhatsApp-এ খুঁজে পাওয়া যায়নি।",
                verified = false
            )
        }
    }

    suspend fun typeMessage(messageText: String): AppActionResult {
        val service = getService()
        if (service == null) {
            return AppActionResult(
                false,
                "বস, ফোনের Accessibility permission চালু করলে আমি নিজে টাইপ করতে পারব।"
            )
        }

        val editField = service.findEditableNode()
        if (editField == null) {
            return AppActionResult(
                false,
                "বস, এখানে টাইপ করার জায়গাটা খুঁজে পাচ্ছি না।"
            )
        }

        val typed = service.typeText(editField, messageText)
        return if (typed) {
            AppActionResult(
                true,
                "বস, মেসেজ লিখেছি: \"$messageText\"",
                verified = true
            )
        } else {
            AppActionResult(
                false,
                "বস, এখানে টাইপ করা সম্ভব হয়নি।"
            )
        }
    }

    fun prepareSendConfirmation(recipient: String, messageText: String): AppActionResult {
        return AppActionResult(
            success = true,
            message = "বস, এই মেসেজটি $recipient-কে পাঠাব?",
            requiresConfirmation = true,
            pendingAction = {
                confirmAndSend()
            }
        )
    }

    suspend fun confirmAndSend(): AppActionResult {
        val service = getService()
        if (service == null) {
            return AppActionResult(
                false,
                "বস, Accessibility Service সক্রিয় না থাকায় মেসেজ স্বয়ংক্রিয়ভাবে পাঠানো গেল না।"
            )
        }

        // Find WhatsApp send button
        val sendButton = service.findNode { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val id = node.viewIdResourceName ?: ""
            desc.contains("send", ignoreCase = true) ||
            desc.contains("পাঠান", ignoreCase = true) ||
            id.contains("send", ignoreCase = true)
        }

        if (sendButton != null && service.clickNode(sendButton)) {
            delay(500)
            return AppActionResult(
                true,
                "বস, মেসেজ পাঠানো হয়েছে।",
                verified = true
            )
        }

        return AppActionResult(
            false,
            "বস, সেন্ড বাটন খুঁজে পাওয়া যায়নি।"
        )
    }
}
