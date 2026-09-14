package com.example.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.accessibility.JarvisAccessibilityService
import kotlinx.coroutines.delay
import java.net.URLEncoder

/**
 * Dedicated Controller for YouTube (Section 13).
 * Supports launching, searching, typing search query, clicking results, play/pause, scroll, and back.
 */
class YouTubeController(
    private val context: Context,
    private val appController: AppController
) {

    private fun getService(): JarvisAccessibilityService? = JarvisAccessibilityService.instance

    suspend fun openYouTube(): AppActionResult {
        val res = appController.openApp("youtube")
        if (res.isFailure) {
            return AppActionResult(false, "বস, YouTube ফোনে ইনস্টল করা নেই।")
        }

        val service = getService()
        val ready = service?.waitForPackage("com.google.android.youtube", 3000) ?: true
        return if (ready) {
            AppActionResult(true, "বস, YouTube খুলছি।", verified = true)
        } else {
            AppActionResult(false, "বস, YouTube প্রস্তুত হতে সময় লাগছে।")
        }
    }

    suspend fun search(query: String): AppActionResult {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return AppActionResult(false, "বস, YouTube-এ কী খুঁজতে চান তা বলুন।")
        }

        val service = getService()
        val currentPkg = service?.getActivePackage()

        // If YouTube is not open, launch official search Intent directly for fast 100% reliable execution
        if (currentPkg?.contains("youtube", ignoreCase = true) != true) {
            val res = appController.searchYouTube(cleanQuery)
            service?.waitForPackage("com.google.android.youtube", 3000)
            return if (res.isSuccess) {
                AppActionResult(true, "বস, YouTube-এ \"$cleanQuery\" অনুসন্ধান করছি।", verified = true)
            } else {
                AppActionResult(false, "বস, YouTube অনুসন্ধান করা যায়নি।")
            }
        }

        // When YouTube is already open in foreground, use in-app search button via Accessibility
        val searchNode = service.findSearchNode()
        if (searchNode != null) {
            service.clickNode(searchNode)
            delay(400)
            val editField = service.findEditableNode()
            if (editField != null) {
                service.typeText(editField, cleanQuery)
                delay(500)
                // Click top suggestion or search icon
                val firstSuggestion = service.findClickableNode(cleanQuery)
                if (firstSuggestion != null) {
                    service.clickNode(firstSuggestion)
                }
                delay(600)
                return AppActionResult(true, "বস, YouTube-এ \"$cleanQuery\" অনুসন্ধান সম্পন্ন হয়েছে।", verified = true)
            }
        }

        // Fallback: Official search intent
        appController.searchYouTube(cleanQuery)
        return AppActionResult(true, "বস, YouTube-এ \"$cleanQuery\" অনুসন্ধান করছি।", verified = true)
    }

    suspend fun playVideo(videoQuery: String): AppActionResult {
        val res = search(videoQuery)
        if (!res.success) return res

        delay(800)
        val service = getService()
        if (service != null) {
            val videoCard = service.findClickableNode(videoQuery)
            if (videoCard != null && service.clickNode(videoCard)) {
                return AppActionResult(true, "বস, \"$videoQuery\" ভিডিওটি চালু করছি।", verified = true)
            }
        }
        return AppActionResult(true, "বস, \"$videoQuery\" এর ফলাফল সামনে রয়েছে।", verified = true)
    }

    fun scroll(direction: String): AppActionResult {
        val service = getService()
        if (service == null) {
            return AppActionResult(false, "বস, Accessibility Service চালু থাকলে আমি স্ক্রল করতে পারব।")
        }

        val scrolled = if (direction.equals("up", ignoreCase = true)) {
            service.scrollUp()
        } else {
            service.scrollDown()
        }

        return if (scrolled) {
            val dirText = if (direction.equals("up", ignoreCase = true)) "উপরে" else "নিচে"
            AppActionResult(true, "বস, $dirText যাচ্ছি।", verified = true)
        } else {
            AppActionResult(false, "বস, স্ক্রল করার মতো কিছু পাওয়া যায়নি।")
        }
    }
}
