package com.example.action

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.ActionResult

class FeedbackController(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun triggerHaptic(result: ActionResult, isHapticEnabled: Boolean) {
        if (!isHapticEnabled || vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (result) {
                    ActionResult.SUCCESS -> {
                        // Crisp short pulse (50ms)
                        vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    ActionResult.FAILED, ActionResult.SERVICE_DISABLED, ActionResult.UNSUPPORTED -> {
                        // Double short alert buzz (80ms - pause 60ms - 80ms)
                        val timings = longArrayOf(0, 80, 60, 80)
                        val amplitudes = intArrayOf(0, 180, 0, 180)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    ActionResult.NOT_ACTIVE, ActionResult.COOLDOWN -> {
                        // Silent / no vibration
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when (result) {
                    ActionResult.SUCCESS -> vibrator.vibrate(40L)
                    ActionResult.FAILED, ActionResult.SERVICE_DISABLED -> vibrator.vibrate(longArrayOf(0, 60, 40, 60), -1)
                    else -> {}
                }
            }
        } catch (_: Exception) {
        }
    }
}
