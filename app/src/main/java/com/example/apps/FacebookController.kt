package com.example.apps

import android.content.Context
import com.example.accessibility.JarvisAccessibilityService
import kotlinx.coroutines.delay

/**
 * Dedicated Controller for Facebook (Section 14).
 */
class FacebookController(
    private val context: Context,
    private val appController: AppController
) {
    private fun getService(): JarvisAccessibilityService? = JarvisAccessibilityService.instance

    suspend fun openFacebook(): AppActionResult {
        val res = appController.openApp("facebook")
        if (res.isFailure) {
            return AppActionResult(false, "বস, Facebook ফোনে ইনস্টল করা নেই।")
        }
        val service = getService()
        val ready = service?.waitForPackage("com.facebook", 3000) ?: true
        return if (ready) {
            AppActionResult(true, "বস, Facebook খুলছি।", verified = true)
        } else {
            AppActionResult(false, "বস, Facebook প্রস্তুত হতে সময় লাগছে।")
        }
    }

    suspend fun search(query: String): AppActionResult {
        val service = getService()
        if (service == null) {
            return AppActionResult(false, "বস, Accessibility চালু থাকলে আমি Facebook-এ সার্চ করতে পারব।")
        }

        val searchBtn = service.findSearchNode()
        if (searchBtn != null) {
            service.clickNode(searchBtn)
            delay(500)
            val editField = service.findEditableNode()
            if (editField != null) {
                service.typeText(editField, query)
                delay(400)
                val suggestion = service.findClickableNode(query)
                if (suggestion != null) service.clickNode(suggestion)
                return AppActionResult(true, "বস, Facebook-এ \"$query\" অনুসন্ধান করা হয়েছে।", verified = true)
            }
        }
        return AppActionResult(false, "বস, Facebook সার্চ বাটন খুঁজে পাওয়া যায়নি।")
    }

    fun scroll(direction: String): AppActionResult {
        val service = getService() ?: return AppActionResult(false, "বস, Accessibility সক্রিয় নেই।")
        val scrolled = if (direction.equals("up", ignoreCase = true)) service.scrollUp() else service.scrollDown()
        return if (scrolled) {
            val dirText = if (direction.equals("up", ignoreCase = true)) "উপরে" else "নিচে"
            AppActionResult(true, "বস, $dirText যাচ্ছি।", verified = true)
        } else {
            AppActionResult(false, "বস, আর স্ক্রল করা যাচ্ছে না।")
        }
    }
}
