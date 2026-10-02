package com.korczak.morok.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MorokAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile private var instance: MorokAccessibilityService? = null
        fun isEnabled() = instance != null
        fun back() = instance?.performGlobalAction(GLOBAL_ACTION_BACK) == true
        fun home() = instance?.performGlobalAction(GLOBAL_ACTION_HOME) == true
        fun recents() = instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) == true
        fun close() = instance?.performGlobalAction(GLOBAL_ACTION_BACK) == true
        fun clickText(text: String) = instance?.findAndClick(text) == true
        fun longClickText(text: String) = instance?.findAndLongClick(text) == true
        fun typeText(text: String) = instance?.typeIntoFocusedField(text) == true
        fun swipe(direction: CommandSwipeDirection) = instance?.dispatchSwipe(direction) == true
        fun scroll(direction: CommandScrollDirection) = instance?.scroll(direction) == true
        fun readScreen(): String = instance?.readActiveWindow() ?: ""
    }

    enum class CommandSwipeDirection { LEFT, RIGHT, UP, DOWN }
    enum class CommandScrollDirection { UP, DOWN }

    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    private fun findNode(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val target = text.trim().lowercase()
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull {
            it.isVisibleToUser &&
                (it.text?.toString()?.lowercase() == target ||
                 it.contentDescription?.toString()?.lowercase() == target)
        } ?: nodes.firstOrNull { it.isVisibleToUser }
    }

    private fun findAndClick(text: String): Boolean = findNode(text)?.let(::clickNode) == true

    private fun findAndLongClick(text: String): Boolean {
        val node = findNode(text) ?: return false
        if (node.isLongClickable && node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) return true
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        return dispatchGestureAt(rect.centerX().toFloat(), rect.centerY().toFloat(), 700L)
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            parent = parent.parent
        }
        return false
    }

    private fun typeIntoFocusedField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun dispatchSwipe(direction: CommandSwipeDirection): Boolean {
        val root = rootInActiveWindow
        val width = resources.displayMetrics.widthPixels.toFloat()
        val height = resources.displayMetrics.heightPixels.toFloat()
        val (sx, sy, ex, ey) = when (direction) {
            CommandSwipeDirection.LEFT -> listOf(width * .8f, height * .5f, width * .2f, height * .5f)
            CommandSwipeDirection.RIGHT -> listOf(width * .2f, height * .5f, width * .8f, height * .5f)
            CommandSwipeDirection.UP -> listOf(width * .5f, height * .8f, width * .5f, height * .2f)
            CommandSwipeDirection.DOWN -> listOf(width * .5f, height * .2f, width * .5f, height * .8f)
        }
        return dispatchGesture(sx, sy, ex, ey, 350L)
    }

    private fun dispatchGestureAt(x: Float, y: Float, duration: Long): Boolean {
        return dispatchGesture(
            x, y, x, y, duration
        )
    }

    private fun dispatchGesture(sx: Float, sy: Float, ex: Float, ey: Float, duration: Long): Boolean {
        val path = Path().apply { moveTo(sx, sy); lineTo(ex, ey) }
        return dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, duration))
                .build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {}
                override fun onCancelled(gestureDescription: GestureDescription?) {}
            },
            null
        )
    }

    private fun scroll(direction: CommandScrollDirection): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollable(root, direction == CommandScrollDirection.DOWN) ?: return false
        val action = if (direction == CommandScrollDirection.DOWN)
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        else
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return scrollable.performAction(action)
    }

    private fun findScrollable(node: AccessibilityNodeInfo, forward: Boolean): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollable(child, forward)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun readActiveWindow(): String {
        val root = rootInActiveWindow ?: return ""
        val out = StringBuilder()
        collectText(root, out)
        return out.toString().trim().take(12000)
    }

    private fun collectText(node: AccessibilityNodeInfo, out: StringBuilder) {
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        val value = if (text.isNotBlank()) text else desc
        if (value.isNotBlank()) {
            if (out.isNotEmpty()) out.append(" | ")
            out.append(value)
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { child ->
            collectText(child, out)
            child.recycle()
        }
    }
}
