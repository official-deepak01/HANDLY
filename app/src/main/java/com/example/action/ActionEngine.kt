package com.example.action

import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.example.model.ActionExecutionOutcome
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.service.AccessibilityController

class ActionEngine(private val context: Context) {

    companion object {
        private const val TAG = "ActionEngine"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun executeAction(action: ActionType): ActionExecutionOutcome {
        Log.i(TAG, "[EXECUTE] Requested action: ${action.displayName}")

        if (action == ActionType.NONE) {
            return ActionExecutionOutcome(ActionResult.SUCCESS, null)
        }

        return when (action) {
            ActionType.NONE -> ActionExecutionOutcome(ActionResult.SUCCESS, null)

            // Navigation Actions (Require real AccessibilityService instance)
            ActionType.BACK -> {
                if (!AccessibilityController.isServiceConnected()) {
                    val err = "Accessibility Service is not connected. Enable it in Settings."
                    Log.w(TAG, "[EXECUTE] BACK failed: $err")
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, err)
                } else {
                    val (success, error) = AccessibilityController.performBack()
                    if (success) {
                        Log.i(TAG, "[EXECUTE] BACK executed successfully.")
                        ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    } else {
                        Log.e(TAG, "[EXECUTE] BACK execution failed: $error")
                        ActionExecutionOutcome(ActionResult.FAILED, error ?: "performGlobalAction(GLOBAL_ACTION_BACK) returned false")
                    }
                }
            }

            ActionType.HOME -> {
                if (!AccessibilityController.isServiceConnected()) {
                    val err = "Accessibility Service is not connected. Enable it in Settings."
                    Log.w(TAG, "[EXECUTE] HOME failed: $err")
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, err)
                } else {
                    val (success, error) = AccessibilityController.performHome()
                    if (success) {
                        Log.i(TAG, "[EXECUTE] HOME executed successfully.")
                        ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    } else {
                        Log.e(TAG, "[EXECUTE] HOME execution failed: $error")
                        ActionExecutionOutcome(ActionResult.FAILED, error ?: "performGlobalAction(GLOBAL_ACTION_HOME) returned false")
                    }
                }
            }

            ActionType.RECENTS -> {
                if (!AccessibilityController.isServiceConnected()) {
                    val err = "Accessibility Service is not connected. Enable it in Settings."
                    Log.w(TAG, "[EXECUTE] RECENTS failed: $err")
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, err)
                } else {
                    val (success, error) = AccessibilityController.performRecents()
                    if (success) {
                        Log.i(TAG, "[EXECUTE] RECENTS executed successfully.")
                        ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    } else {
                        Log.e(TAG, "[EXECUTE] RECENTS execution failed: $error")
                        ActionExecutionOutcome(ActionResult.FAILED, error ?: "performGlobalAction(GLOBAL_ACTION_RECENTS) returned false")
                    }
                }
            }

            // Scrolling Actions (Require Accessibility)
            ActionType.SCROLL_FORWARD -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performScroll(forward = true, horizontal = false)
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Scroll forward failed")
                }
            }

            ActionType.SCROLL_BACKWARD -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performScroll(forward = false, horizontal = false)
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Scroll backward failed")
                }
            }

            ActionType.SCROLL_LEFT -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performScroll(forward = false, horizontal = true)
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Scroll left failed")
                }
            }

            ActionType.SCROLL_RIGHT -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performScroll(forward = true, horizontal = true)
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Scroll right failed")
                }
            }

            // Touch Actions (Require Accessibility)
            ActionType.TAP -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performTap()
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Tap gesture failed")
                }
            }

            ActionType.DOUBLE_TAP -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performDoubleTap()
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Double tap failed")
                }
            }

            ActionType.LONG_PRESS -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performLongPress()
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Long press failed")
                }
            }

            ActionType.TOUCH_SWIPE_UP -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val dm = Resources.getSystem().displayMetrics
                    val w = dm.widthPixels.toFloat()
                    val h = dm.heightPixels.toFloat()
                    val (success, error) = AccessibilityController.performDrag(w * 0.5f, h * 0.8f, w * 0.5f, h * 0.2f, 200L)
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Touch swipe up failed")
                }
            }

            ActionType.TOUCH_SWIPE_DOWN -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val dm = Resources.getSystem().displayMetrics
                    val w = dm.widthPixels.toFloat()
                    val h = dm.heightPixels.toFloat()
                    val (success, error) = AccessibilityController.performDrag(w * 0.5f, h * 0.2f, w * 0.5f, h * 0.8f, 200L)
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Touch swipe down failed")
                }
            }

            ActionType.SWIPE_LEFT, ActionType.TOUCH_SWIPE_LEFT -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val dm = Resources.getSystem().displayMetrics
                    val w = dm.widthPixels.toFloat()
                    val h = dm.heightPixels.toFloat()
                    val (success, error) = AccessibilityController.performDrag(w * 0.85f, h * 0.5f, w * 0.15f, h * 0.5f, 200L)
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Swipe left failed")
                }
            }

            ActionType.SWIPE_RIGHT, ActionType.TOUCH_SWIPE_RIGHT -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val dm = Resources.getSystem().displayMetrics
                    val w = dm.widthPixels.toFloat()
                    val h = dm.heightPixels.toFloat()
                    val (success, error) = AccessibilityController.performDrag(w * 0.15f, h * 0.5f, w * 0.85f, h * 0.5f, 200L)
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Swipe right failed")
                }
            }

            // Media Controls (Via AudioManager / MediaKeyEvent)
            ActionType.PLAY_PAUSE -> sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            ActionType.NEXT_TRACK -> sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
            ActionType.PREV_TRACK -> sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

            // Volume Controls
            ActionType.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            ActionType.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
            ActionType.MUTE -> adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE)

            // Camera Shutter
            ActionType.CAMERA_SHUTTER -> sendKeyStroke(KeyEvent.KEYCODE_VOLUME_UP)

            // System Actions
            ActionType.NOTIFICATIONS -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performNotifications()
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Notifications action failed")
                }
            }

            ActionType.QUICK_SETTINGS -> {
                if (!AccessibilityController.isServiceConnected()) {
                    ActionExecutionOutcome(ActionResult.SERVICE_DISABLED, "Accessibility Service is not connected")
                } else {
                    val (success, error) = AccessibilityController.performQuickSettings()
                    if (success) ActionExecutionOutcome(ActionResult.SUCCESS, null)
                    else ActionExecutionOutcome(ActionResult.FAILED, error ?: "Quick Settings action failed")
                }
            }
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int): ActionExecutionOutcome {
        val am = audioManager ?: return ActionExecutionOutcome(ActionResult.UNSUPPORTED, "AudioManager service not available")
        return try {
            val eventTime = SystemClock.uptimeMillis()
            val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
            am.dispatchMediaKeyEvent(down)
            am.dispatchMediaKeyEvent(up)
            ActionExecutionOutcome(ActionResult.SUCCESS, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed dispatching media key: ${e.message}", e)
            ActionExecutionOutcome(ActionResult.FAILED, e.message ?: "Media key dispatch failed")
        }
    }

    private fun adjustVolume(direction: Int): ActionExecutionOutcome {
        val am = audioManager ?: return ActionExecutionOutcome(ActionResult.UNSUPPORTED, "AudioManager service not available")
        return try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            ActionExecutionOutcome(ActionResult.SUCCESS, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed adjusting volume: ${e.message}", e)
            ActionExecutionOutcome(ActionResult.FAILED, e.message ?: "Adjust volume failed")
        }
    }

    private fun sendKeyStroke(keyCode: Int): ActionExecutionOutcome {
        val am = audioManager ?: return ActionExecutionOutcome(ActionResult.UNSUPPORTED, "AudioManager service not available")
        return try {
            val eventTime = SystemClock.uptimeMillis()
            val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
            am.dispatchMediaKeyEvent(down)
            am.dispatchMediaKeyEvent(up)
            ActionExecutionOutcome(ActionResult.SUCCESS, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending keystroke: ${e.message}", e)
            ActionExecutionOutcome(ActionResult.FAILED, e.message ?: "Keystroke failed")
        }
    }
}
