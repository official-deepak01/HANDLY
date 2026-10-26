package com.example.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.example.R
import com.example.model.ControlMode

class FloatingOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cursorView: View? = null
    private var cursorParams: WindowManager.LayoutParams? = null

    private var statusPillView: View? = null
    private var statusPillParams: WindowManager.LayoutParams? = null

    private var isCursorAttached = false
    private var isStatusPillAttached = false
    private var currentMode = ControlMode.GESTURE

    fun updateMode(mode: ControlMode) {
        currentMode = mode
        mainHandler.post {
            if (mode == ControlMode.AIR_CURSOR) {
                removeStatusPill()
                ensureCursorView()
            } else {
                removeCursorView()
                ensureStatusPill()
            }
        }
    }

    fun updateCursor(screenX: Float, screenY: Float, isPinching: Boolean, isClicking: Boolean) {
        if (!hasOverlayPermission() || currentMode != ControlMode.AIR_CURSOR) return
        mainHandler.post {
            val view = cursorView ?: return@post
            val params = cursorParams ?: return@post
            if (!isCursorAttached) return@post

            // Center the 36dp cursor view over the point
            val cursorHalfSize = (36 * context.resources.displayMetrics.density / 2f).toInt()
            params.x = (screenX.toInt() - cursorHalfSize).coerceAtLeast(0)
            params.y = (screenY.toInt() - cursorHalfSize).coerceAtLeast(0)

            try {
                windowManager?.updateViewLayout(view, params)
                // Visual feedback on the cursor
                val ring = view.findViewById<View>(R.id.cursor_ring)
                val dot = view.findViewById<View>(R.id.cursor_dot)
                if (ring != null && dot != null) {
                    if (isClicking) {
                        ring.scaleX = 1.4f
                        ring.scaleY = 1.4f
                        ring.alpha = 1.0f
                    } else if (isPinching) {
                        ring.scaleX = 0.8f
                        ring.scaleY = 0.8f
                        ring.alpha = 0.9f
                    } else {
                        ring.scaleX = 1.0f
                        ring.scaleY = 1.0f
                        ring.alpha = 0.6f
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating cursor layout: ${e.message}")
            }
        }
    }

    fun updateStatus(gestureName: String?, profileName: String) {
        if (!hasOverlayPermission() || currentMode == ControlMode.AIR_CURSOR) return
        mainHandler.post {
            val pill = statusPillView ?: return@post
            if (!isStatusPillAttached) return@post
            val text = pill.findViewById<TextView>(R.id.overlay_status_text)
            text?.text = if (gestureName != null) "$profileName: $gestureName" else "$profileName: Active"
        }
    }

    private fun ensureCursorView() {
        if (!hasOverlayPermission() || isCursorAttached) return
        try {
            val inflater = LayoutInflater.from(context)
            cursorView = inflater.inflate(R.layout.overlay_cursor, null)

            val sizePx = (36 * context.resources.displayMetrics.density).toInt()
            cursorParams = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 200
                y = 400
            }

            windowManager?.addView(cursorView, cursorParams)
            isCursorAttached = true
            Log.i(TAG, "Air Cursor floating overlay attached successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed attaching Air Cursor overlay: ${e.message}", e)
            isCursorAttached = false
        }
    }

    private fun ensureStatusPill() {
        if (!hasOverlayPermission() || isStatusPillAttached) return
        try {
            val inflater = LayoutInflater.from(context)
            statusPillView = inflater.inflate(R.layout.overlay_status_pill, null)

            statusPillParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (48 * context.resources.displayMetrics.density).toInt()
            }

            windowManager?.addView(statusPillView, statusPillParams)
            isStatusPillAttached = true
            Log.i(TAG, "Status pill floating overlay attached successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed attaching status pill overlay: ${e.message}", e)
            isStatusPillAttached = false
        }
    }

    private fun removeCursorView() {
        if (isCursorAttached && cursorView != null) {
            try {
                windowManager?.removeView(cursorView)
            } catch (_: Exception) {}
            isCursorAttached = false
            cursorView = null
            cursorParams = null
        }
    }

    private fun removeStatusPill() {
        if (isStatusPillAttached && statusPillView != null) {
            try {
                windowManager?.removeView(statusPillView)
            } catch (_: Exception) {}
            isStatusPillAttached = false
            statusPillView = null
            statusPillParams = null
        }
    }

    fun destroy() {
        mainHandler.post {
            removeCursorView()
            removeStatusPill()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}
