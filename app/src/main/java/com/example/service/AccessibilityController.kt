package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessibilityController {

    private const val TAG = "AccessibilityController"
    private var activeService: GestureAccessibilityService? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    fun setService(service: GestureAccessibilityService?) {
        activeService = service
        val connected = (service != null)
        _isConnected.value = connected
        Log.i(TAG, "[CONNECTION] Accessibility service set. Instance: $service, isConnected: $connected")
    }

    fun updateForegroundPackage(pkg: String?) {
        if (!pkg.isNullOrBlank() && pkg != _foregroundPackage.value) {
            _foregroundPackage.value = pkg
            Log.d(TAG, "[FOREGROUND] Foreground package changed to: $pkg")
        }
    }

    /**
     * Strictly verifies that the real Android AccessibilityService instance exists.
     * Never returns true if activeService is null.
     */
    fun isServiceConnected(): Boolean {
        val connected = (activeService != null)
        if (_isConnected.value != connected) {
            _isConnected.value = connected
        }
        return connected
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            Log.i(TAG, "Opening Android Accessibility Settings...")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings: ${e.message}", e)
        }
    }

    fun performBack(): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performBack() requested")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected. Enable it in Settings."
            Log.w(TAG, "[ACTION_RES] performBack() failed: $err")
            return Pair(false, err)
        }
        return try {
            val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            Log.i(TAG, "[ACTION_RES] performGlobalAction(GLOBAL_ACTION_BACK) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "System performGlobalAction(GLOBAL_ACTION_BACK) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performBack: ${e.message}", e)
            Pair(false, e.message ?: "Exception in performBack")
        }
    }

    fun performHome(): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performHome() requested")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected. Enable it in Settings."
            Log.w(TAG, "[ACTION_RES] performHome() failed: $err")
            return Pair(false, err)
        }
        return try {
            val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            Log.i(TAG, "[ACTION_RES] performGlobalAction(GLOBAL_ACTION_HOME) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "System performGlobalAction(GLOBAL_ACTION_HOME) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performHome: ${e.message}", e)
            Pair(false, e.message ?: "Exception in performHome")
        }
    }

    fun performRecents(): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performRecents() requested")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected. Enable it in Settings."
            Log.w(TAG, "[ACTION_RES] performRecents() failed: $err")
            return Pair(false, err)
        }
        return try {
            val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            Log.i(TAG, "[ACTION_RES] performGlobalAction(GLOBAL_ACTION_RECENTS) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "System performGlobalAction(GLOBAL_ACTION_RECENTS) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performRecents: ${e.message}", e)
            Pair(false, e.message ?: "Exception in performRecents")
        }
    }

    @Suppress("DEPRECATION")
    fun performScroll(forward: Boolean, horizontal: Boolean = false): Pair<Boolean, String?> {
        val actionName = if (horizontal) (if (forward) "SCROLL_RIGHT" else "SCROLL_LEFT") else (if (forward) "SCROLL_FORWARD" else "SCROLL_BACKWARD")
        Log.d(TAG, "[ACTION_REQ] performScroll: $actionName requested")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected."
            Log.w(TAG, "[ACTION_RES] $actionName failed: $err")
            return Pair(false, err)
        }

        try {
            val rootNode = service.rootInActiveWindow
            if (rootNode != null) {
                val action = if (horizontal) {
                    if (forward) AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT
                    else AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT
                } else {
                    if (forward) AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD
                    else AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD
                }

                val scrollableNode = findScrollableNode(rootNode)
                if (scrollableNode != null) {
                    val success = scrollableNode.performAction(action.id)
                    scrollableNode.recycle()
                    rootNode.recycle()
                    Log.i(TAG, "[ACTION_RES] Node scroll performAction result: $success")
                    if (success) return Pair(true, null)
                } else {
                    rootNode.recycle()
                }
            }

            // Fallback: Dispatch physical touch drag gesture
            Log.d(TAG, "[ACTION_REQ] Node scroll unavailable, executing gesture drag fallback for $actionName")
            val displayMetrics = service.resources.displayMetrics
            val width = displayMetrics.widthPixels.toFloat()
            val height = displayMetrics.heightPixels.toFloat()

            return if (horizontal) {
                val startX = if (forward) width * 0.8f else width * 0.2f
                val endX = if (forward) width * 0.2f else width * 0.8f
                performDrag(startX, height * 0.5f, endX, height * 0.5f, 250L)
            } else {
                val startY = if (forward) height * 0.75f else height * 0.25f
                val endY = if (forward) height * 0.25f else height * 0.75f
                performDrag(width * 0.5f, startY, width * 0.5f, endY, 250L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception during performScroll: ${e.message}", e)
            return Pair(false, e.message ?: "Exception during scroll")
        }
    }

    fun performTap(screenX: Float? = null, screenY: Float? = null): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performTap requested at ($screenX, $screenY)")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected."
            Log.w(TAG, "[ACTION_RES] performTap failed: $err")
            return Pair(false, err)
        }

        return try {
            val metrics = service.resources.displayMetrics
            val x = screenX ?: (metrics.widthPixels * 0.5f)
            val y = screenY ?: (metrics.heightPixels * 0.5f)

            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 80L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            val result = service.dispatchGesture(gesture, null, null)
            Log.i(TAG, "[ACTION_RES] dispatchGesture(TAP) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "dispatchGesture(TAP) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performTap: ${e.message}", e)
            Pair(false, e.message ?: "Exception during tap")
        }
    }

    fun performNotifications(): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performNotifications() requested")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected."
            Log.w(TAG, "[ACTION_RES] performNotifications() failed: $err")
            return Pair(false, err)
        }
        return try {
            val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            Log.i(TAG, "[ACTION_RES] performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "System performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performNotifications: ${e.message}", e)
            Pair(false, e.message ?: "Exception in performNotifications")
        }
    }

    fun performQuickSettings(): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performQuickSettings() requested")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected."
            Log.w(TAG, "[ACTION_RES] performQuickSettings() failed: $err")
            return Pair(false, err)
        }
        return try {
            val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            Log.i(TAG, "[ACTION_RES] performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "System performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performQuickSettings: ${e.message}", e)
            Pair(false, e.message ?: "Exception in performQuickSettings")
        }
    }

    fun performDoubleTap(screenX: Float? = null, screenY: Float? = null): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performDoubleTap requested at ($screenX, $screenY)")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected."
            Log.w(TAG, "[ACTION_RES] performDoubleTap failed: $err")
            return Pair(false, err)
        }

        return try {
            val metrics = service.resources.displayMetrics
            val x = screenX ?: (metrics.widthPixels * 0.5f)
            val y = screenY ?: (metrics.heightPixels * 0.5f)

            val path = Path().apply { moveTo(x, y) }
            val stroke1 = GestureDescription.StrokeDescription(path, 0L, 60L)
            val stroke2 = GestureDescription.StrokeDescription(path, 120L, 60L)
            val gesture = GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2).build()
            val result = service.dispatchGesture(gesture, null, null)
            Log.i(TAG, "[ACTION_RES] dispatchGesture(DOUBLE_TAP) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "dispatchGesture(DOUBLE_TAP) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performDoubleTap: ${e.message}", e)
            Pair(false, e.message ?: "Exception during double tap")
        }
    }

    fun performLongPress(screenX: Float? = null, screenY: Float? = null, durationMs: Long = 600L): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performLongPress requested at ($screenX, $screenY)")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected."
            Log.w(TAG, "[ACTION_RES] performLongPress failed: $err")
            return Pair(false, err)
        }

        return try {
            val metrics = service.resources.displayMetrics
            val x = screenX ?: (metrics.widthPixels * 0.5f)
            val y = screenY ?: (metrics.heightPixels * 0.5f)

            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(400L))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            val result = service.dispatchGesture(gesture, null, null)
            Log.i(TAG, "[ACTION_RES] dispatchGesture(LONG_PRESS) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "dispatchGesture(LONG_PRESS) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performLongPress: ${e.message}", e)
            Pair(false, e.message ?: "Exception during long press")
        }
    }

    fun performDrag(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): Pair<Boolean, String?> {
        Log.d(TAG, "[ACTION_REQ] performDrag requested from ($startX, $startY) to ($endX, $endY)")
        val service = activeService
        if (service == null) {
            val err = "Accessibility Service is not connected."
            Log.w(TAG, "[ACTION_RES] performDrag failed: $err")
            return Pair(false, err)
        }

        return try {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(100L))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            val result = service.dispatchGesture(gesture, null, null)
            Log.i(TAG, "[ACTION_RES] dispatchGesture(DRAG) result: $result")
            if (result) Pair(true, null)
            else Pair(false, "dispatchGesture(DRAG) returned false")
        } catch (e: Exception) {
            Log.e(TAG, "[ACTION_RES] Exception in performDrag: ${e.message}", e)
            Pair(false, e.message ?: "Exception during drag")
        }
    }

    @Suppress("DEPRECATION")
    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
            child.recycle()
        }
        return null
    }
}
