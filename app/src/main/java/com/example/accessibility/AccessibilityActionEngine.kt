package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Universal Accessibility Engine for Rashed AI.
 * Implements Section 10 & 11: Reusable UI Automation Engine using Android Accessibility Nodes.
 * No hardcoded screen coordinates; operates on visible UI hierarchy, editable fields,
 * search buttons, and scrollable containers.
 */
object AccessibilityActionEngine {

    private fun getService(): RashedAccessibilityService? = RashedAccessibilityService.instance

    private fun getRootNode(): AccessibilityNodeInfo? = getService()?.rootInActiveWindow

    fun isServiceActive(): Boolean = RashedAccessibilityService.isRunning()

    fun goBack(): Boolean {
        return getService()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) ?: false
    }

    fun goHome(): Boolean {
        return getService()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) ?: false
    }

    fun lockScreen(): Boolean {
        return getService()?.lockScreen() ?: false
    }

    fun findText(target: String, matchCase: Boolean = false): AccessibilityNodeInfo? {
        val root = getRootNode() ?: return null
        return searchTree(root) { node ->
            val text = node.text?.toString() ?: ""
            if (matchCase) text.contains(target) else text.contains(target, ignoreCase = true)
        }
    }

    fun findContentDescription(desc: String): AccessibilityNodeInfo? {
        val root = getRootNode() ?: return null
        return searchTree(root) { node ->
            val contentDesc = node.contentDescription?.toString() ?: ""
            contentDesc.contains(desc, ignoreCase = true)
        }
    }

    fun findClickableElement(textOrDesc: String): AccessibilityNodeInfo? {
        val root = getRootNode() ?: return null
        return searchTree(root) { node ->
            val matches = (node.text?.toString()?.contains(textOrDesc, ignoreCase = true) == true) ||
                    (node.contentDescription?.toString()?.contains(textOrDesc, ignoreCase = true) == true)
            if (!matches) return@searchTree false

            var curr: AccessibilityNodeInfo? = node
            while (curr != null) {
                if (curr.isClickable) return@searchTree true
                curr = curr.parent
            }
            false
        }
    }

    fun findEditableField(): AccessibilityNodeInfo? {
        val root = getRootNode() ?: return null
        return searchTree(root) { node ->
            node.isEditable || node.className?.toString()?.contains("EditText", ignoreCase = true) == true
        }
    }

    fun click(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        var target: AccessibilityNodeInfo? = node
        while (target != null) {
            if (target.isClickable) {
                return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            target = target.parent
        }
        return false
    }

    fun clickByText(text: String): Boolean {
        val node = findClickableElement(text) ?: findText(text)
        return click(node)
    }

    fun typeText(node: AccessibilityNodeInfo?, text: String): Boolean {
        val target = node ?: findEditableField() ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    fun clearText(node: AccessibilityNodeInfo?): Boolean {
        return typeText(node, "")
    }

    fun scrollDown(): Boolean {
        val root = getRootNode() ?: return false
        val scrollable = searchTree(root) { it.isScrollable } ?: root
        return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun scrollUp(): Boolean {
        val root = getRootNode() ?: return false
        val scrollable = searchTree(root) { it.isScrollable } ?: root
        return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    fun longClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        var target: AccessibilityNodeInfo? = node
        while (target != null) {
            if (target.isLongClickable) {
                return target.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            }
            target = target.parent
        }
        return false
    }

    fun readVisibleText(): List<String> {
        val root = getRootNode() ?: return emptyList()
        val results = mutableListOf<String>()
        collectAllText(root, results)
        return results
    }

    private fun collectAllText(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank() && !list.contains(text)) {
            list.add(text)
        }
        if (!desc.isNullOrBlank() && !list.contains(desc)) {
            list.add(desc)
        }

        for (i in 0 until node.childCount) {
            collectAllText(node.getChild(i), list)
        }
    }

    fun findAndClickSearchBox(): Boolean {
        val root = getRootNode() ?: return false
        // Search icon or search button
        val searchIcon = searchTree(root) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            desc.contains("search", ignoreCase = true) ||
                    desc.contains("খুঁজুন", ignoreCase = true) ||
                    text.contains("search", ignoreCase = true) ||
                    text.contains("Search YouTube", ignoreCase = true)
        }

        if (searchIcon != null && click(searchIcon)) {
            return true
        }

        val editField = findEditableField()
        if (editField != null) {
            return click(editField)
        }
        return false
    }

    fun clickMatchingVideo(titleQuery: String): Boolean {
        val root = getRootNode() ?: return false
        // Find visible video cards or titles matching query
        val words = titleQuery.lowercase().split("\\s+".toRegex()).filter { it.length > 2 }

        val match = searchTree(root) { node ->
            val text = (node.text?.toString() ?: "").lowercase()
            val desc = (node.contentDescription?.toString() ?: "").lowercase()

            val matchesAny = words.any { text.contains(it) || desc.contains(it) }
            val isLikelyVideo = desc.contains("views") || desc.contains("ago") || desc.contains("watch") ||
                    text.isNotBlank() && (node.isClickable || node.parent?.isClickable == true)
            matchesAny && isLikelyVideo
        }

        if (match != null) {
            return click(match)
        }

        // Fallback: click first matching clickable text element
        for (w in words) {
            val fallbackNode = findClickableElement(w)
            if (fallbackNode != null && click(fallbackNode)) {
                return true
            }
        }
        return false
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
