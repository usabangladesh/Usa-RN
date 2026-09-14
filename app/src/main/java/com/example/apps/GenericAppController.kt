package com.example.apps

import android.content.Context
import com.example.accessibility.JarvisAccessibilityService
import kotlinx.coroutines.delay

/**
 * GenericAppController (Section 17).
 * Universal controller for all compatible Android apps.
 * Operates safely over the active Accessibility tree:
 * Open -> Detect -> Navigate -> Search -> Type -> Click -> Scroll -> Back -> Verify
 */
class GenericAppController(
    private val context: Context,
    private val appController: AppController
) {

    private fun getService(): JarvisAccessibilityService? = JarvisAccessibilityService.instance

    suspend fun openApp(appName: String): AppActionResult {
        val res = appController.openApp(appName)
        if (res.isFailure) {
            return AppActionResult(
                false,
                "বস, $appName ফোনে ইনস্টল করা নেই বা খোলা যায়নি।"
            )
        }

        val service = getService()
        val targetPkg = appController.findApp(appName)
        if (service != null && targetPkg != null) {
            service.waitForPackage(targetPkg, 3000)
        }

        return AppActionResult(
            true,
            "বস, $appName খুলছি।",
            verified = true
        )
    }

    suspend fun universalType(text: String): AppActionResult {
        val service = getService()
        if (service == null) {
            return AppActionResult(
                false,
                "বস, ফোনের Accessibility permission চালু করলে আমি এখানে টাইপ করতে পারব।"
            )
        }

        val editableNode = service.findEditableNode()
        if (editableNode == null) {
            return AppActionResult(
                false,
                "বস, এখানে টাইপ করার জায়গাটা খুঁজে পাচ্ছি না।"
            )
        }

        val typed = service.typeText(editableNode, text)
        return if (typed) {
            AppActionResult(
                true,
                "বস, এখানে টাইপ করেছি: \"$text\"",
                verified = true
            )
        } else {
            AppActionResult(
                false,
                "বস, টাইপ করার চেষ্টা ব্যর্থ হয়েছে।"
            )
        }
    }

    suspend fun universalSearch(query: String): AppActionResult {
        val service = getService()
        if (service == null) {
            return AppActionResult(
                false,
                "বস, Accessibility Service চালু থাকলে আমি সার্চ করতে পারব।"
            )
        }

        val searchNode = service.findSearchNode()
        if (searchNode == null) {
            return AppActionResult(
                false,
                "বস, সার্চ করার অপশনটি এখন খুঁজে পাচ্ছি না।"
            )
        }

        service.clickNode(searchNode)
        delay(400)

        if (query.isNotBlank()) {
            val editField = service.findEditableNode()
            if (editField != null) {
                service.typeText(editField, query)
                delay(500)
                val suggestion = service.findClickableNode(query)
                if (suggestion != null) service.clickNode(suggestion)
                return AppActionResult(
                    true,
                    "বস, \"$query\" অনুসন্ধান করা হচ্ছে।",
                    verified = true
                )
            }
        }

        return AppActionResult(
            true,
            "বস, Search অপশন খুলে দিয়েছি।",
            verified = true
        )
    }

    fun universalClick(targetText: String): AppActionResult {
        val service = getService()
        if (service == null) {
            return AppActionResult(
                false,
                "বস, Accessibility সক্রিয় না থাকলে স্ক্রিনে ক্লিক করা সম্ভব নয়।"
            )
        }

        val clicked = service.clickByText(targetText)
        return if (clicked) {
            AppActionResult(
                true,
                "বস, \"$targetText\"-এ ক্লিক করেছি।",
                verified = true
            )
        } else {
            AppActionResult(
                false,
                "বস, স্ক্রিনে \"$targetText\" অপশনটি খুঁজে পাচ্ছি না।"
            )
        }
    }

    fun universalScroll(direction: String): AppActionResult {
        val service = getService()
        if (service == null) {
            return AppActionResult(
                false,
                "বস, Accessibility Service সক্রিয় নেই।"
            )
        }

        val isUp = direction.equals("up", ignoreCase = true) || direction.contains("উপরে")
        val scrolled = if (isUp) service.scrollUp() else service.scrollDown()

        return if (scrolled) {
            val label = if (isUp) "উপরে" else "নিচে"
            AppActionResult(true, "বস, $label যাচ্ছি।", verified = true)
        } else {
            AppActionResult(false, "বস, আর স্ক্রল করার মতো জায়গা নেই।")
        }
    }

    fun universalBack(): AppActionResult {
        val service = getService()
        val backed = service?.goBack() ?: false
        return if (backed) {
            AppActionResult(true, "বস, ব্যাক করেছি।", verified = true)
        } else {
            AppActionResult(false, "বস, আগের পেজে যাওয়া সম্ভব হয়নি।")
        }
    }
}
