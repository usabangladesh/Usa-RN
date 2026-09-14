package com.example.apps

import android.content.Context
import com.example.accessibility.JarvisAccessibilityService
import kotlinx.coroutines.delay

/**
 * Dedicated Controller for TikTok (Section 15).
 */
class TikTokController(
    private val context: Context,
    private val appController: AppController
) {
    private fun getService(): JarvisAccessibilityService? = JarvisAccessibilityService.instance

    suspend fun openTikTok(): AppActionResult {
        val res = appController.openApp("tiktok")
        if (res.isFailure) {
            return AppActionResult(false, "বস, TikTok ফোনে ইনস্টল করা নেই।")
        }
        val service = getService()
        service?.waitForPackage("zhiliaoapp.musically", 3000)
        return AppActionResult(true, "বস, TikTok খুলছি।", verified = true)
    }

    suspend fun search(query: String): AppActionResult {
        val service = getService() ?: return AppActionResult(false, "বস, Accessibility সক্রিয় নেই।")
        val searchNode = service.findSearchNode()
        if (searchNode != null) {
            service.clickNode(searchNode)
            delay(400)
            val editField = service.findEditableNode()
            if (editField != null) {
                service.typeText(editField, query)
                delay(500)
                val suggestion = service.findClickableNode(query)
                if (suggestion != null) service.clickNode(suggestion)
                return AppActionResult(true, "বস, TikTok-এ \"$query\" সার্চ করা হয়েছে।", verified = true)
            }
        }
        return AppActionResult(false, "বস, TikTok সার্চ ফিল্ড খুঁজে পাওয়া যায়নি।")
    }

    fun scroll(direction: String): AppActionResult {
        val service = getService() ?: return AppActionResult(false, "বস, Accessibility সক্রিয় নেই।")
        val scrolled = if (direction.equals("up", ignoreCase = true)) service.scrollUp() else service.scrollDown()
        return if (scrolled) {
            val dirText = if (direction.equals("up", ignoreCase = true)) "উপরে" else "নিচে"
            AppActionResult(true, "বস, $dirText যাচ্ছি।", verified = true)
        } else {
            AppActionResult(false, "বস, স্ক্রল করা সম্ভব হয়নি।")
        }
    }
}
