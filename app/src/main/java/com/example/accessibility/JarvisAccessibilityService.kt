package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

/**
 * JarvisAccessibilityService
 * Real Accessibility Service for Android UI Automation, Context Tracking & Verification.
 * Strictly operates only on visible and authorized UI nodes.
 */
open class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null
            protected set

        fun isRunning(): Boolean = instance != null

        @Volatile
        var currentForegroundPackage: String? = null
            protected set

        @Volatile
        var currentActivityName: String? = null
            protected set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        RashedAccessibilityService.instance = this as? RashedAccessibilityService ?: RashedAccessibilityService.instance
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString()
                if (!pkg.isNullOrBlank()) {
                    currentForegroundPackage = pkg
                }
                val cls = event.className?.toString()
                if (!cls.isNullOrBlank()) {
                    currentActivityName = cls
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val pkg = event.packageName?.toString()
                if (!pkg.isNullOrBlank() && currentForegroundPackage == null) {
                    currentForegroundPackage = pkg
                }
            }
        }
    }

    override fun onInterrupt() {
        // Interruption hook
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun getActivePackage(): String? {
        val root = rootInActiveWindow
        return root?.packageName?.toString() ?: currentForegroundPackage
    }

    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return searchTree(root, predicate)
    }

    fun findClickableNode(targetTextOrDesc: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return searchTree(root) { node ->
            val textMatch = node.text?.toString()?.contains(targetTextOrDesc, ignoreCase = true) == true
            val descMatch = node.contentDescription?.toString()?.contains(targetTextOrDesc, ignoreCase = true) == true
            if (!textMatch && !descMatch) return@searchTree false

            var curr: AccessibilityNodeInfo? = node
            while (curr != null) {
                if (curr.isClickable) return@searchTree true
                curr = curr.parent
            }
            false
        }
    }

    fun findEditableNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return searchTree(root) { node ->
            node.isEditable || node.className?.toString()?.contains("EditText", ignoreCase = true) == true
        }
    }

    fun findSearchNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val keywords = listOf("search", "খুঁজুন", "অনুসন্ধান", "search_src_text", "search_button", "menuitem_search")
        return searchTree(root) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""
            val matchesKeyword = keywords.any {
                desc.contains(it, ignoreCase = true) ||
                text.contains(it, ignoreCase = true) ||
                viewId.contains(it, ignoreCase = true)
            }
            matchesKeyword && (node.isClickable || node.isEditable || node.parent?.isClickable == true)
        } ?: findEditableNode()
    }

    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isClickable) {
                return curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            curr = curr.parent
        }
        return false
    }

    fun clickByText(text: String): Boolean {
        val node = findClickableNode(text) ?: findNode {
            it.text?.toString()?.contains(text, ignoreCase = true) == true ||
            it.contentDescription?.toString()?.contains(text, ignoreCase = true) == true
        }
        return clickNode(node)
    }

    fun typeText(node: AccessibilityNodeInfo?, text: String): Boolean {
        val target = node ?: findEditableNode() ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = searchTree(root) { it.isScrollable } ?: root
        return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun scrollUp(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = searchTree(root) { it.isScrollable } ?: root
        return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    suspend fun waitForPackage(expectedPackage: String, timeoutMs: Long = 3000): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val current = getActivePackage()
            if (current != null && current.contains(expectedPackage, ignoreCase = true)) {
                return true
            }
            delay(150)
        }
        return false
    }

    suspend fun waitForCondition(timeoutMs: Long = 3000, condition: () -> Boolean): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return true
            delay(150)
        }
        return false
    }

    fun extractScreenHierarchyText(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val list = mutableListOf<String>()
        collectText(root, list, 0)
        return list
    }

    private fun collectText(node: AccessibilityNodeInfo?, list: MutableList<String>, depth: Int) {
        if (node == null || depth > 10) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank() && !list.contains(text)) list.add(text)
        if (!desc.isNullOrBlank() && !list.contains(desc)) list.add(desc)

        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), list, depth + 1)
        }
    }

    private fun searchTree(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = searchTree(child, predicate)
            if (found != null) return found
        }
        return null
    }
}
