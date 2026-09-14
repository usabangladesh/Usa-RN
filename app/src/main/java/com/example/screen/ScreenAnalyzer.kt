package com.example.screen

import android.content.Context
import com.example.accessibility.RashedAccessibilityService

class ScreenAnalyzer(private val context: Context) {

    fun analyzeVisibleScreen(): Result<String> {
        val service = RashedAccessibilityService.instance
        if (service == null) {
            return Result.failure(
                Exception("Screen Understanding এর জন্য Accessibility Service চালু থাকা আবশ্যক।")
            )
        }

        val extractedText = service.extractScreenHierarchyText().joinToString("\n")
        return Result.success(
            "Current Screen UI Content:\n$extractedText"
        )
    }

    fun takeScreenshot(): Result<String> {
        val service = RashedAccessibilityService.instance
        if (service == null) {
            return Result.failure(Exception("Screenshot নেওয়ার জন্য Accessibility Service প্রয়োজন।"))
        }

        val success = service.takeScreenshot()
        return if (success) {
            Result.success("Screenshot গ্রহণ করা হয়েছে।")
        } else {
            Result.failure(Exception("এই Android সংস্করণে Accessibility Screenshot সমর্থিত নয়।"))
        }
    }
}
