package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class GestureAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GestureAccessService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "[LIFECYCLE] GestureAccessibilityService onCreate() called.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "[LIFECYCLE] GestureAccessibilityService onServiceConnected() - Real service connected to Android OS.")
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 100
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            serviceInfo = info
            Log.i(TAG, "[CONFIG] Accessibility serviceInfo updated. flags=${info.flags}")
        } catch (e: Exception) {
            Log.e(TAG, "[CONFIG] Error updating serviceInfo in onServiceConnected: ${e.message}", e)
        }
        AccessibilityController.setService(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType
        Log.d(TAG, "[EVENT] onAccessibilityEvent: type=$eventType, pkg=${event.packageName}")
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank() && pkg != packageName) {
                AccessibilityController.updateForegroundPackage(pkg)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "[LIFECYCLE] GestureAccessibilityService onInterrupt() called.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "[LIFECYCLE] GestureAccessibilityService onUnbind() - Disconnecting active service.")
        AccessibilityController.setService(null)
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.i(TAG, "[LIFECYCLE] GestureAccessibilityService onRebind() - Reconnecting active service.")
        AccessibilityController.setService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "[LIFECYCLE] GestureAccessibilityService onDestroy() - Service destroyed.")
        AccessibilityController.setService(null)
    }
}
