package com.example.vision

import com.example.model.BatteryMode
import com.example.model.GestureLifecycleState
import com.example.service.ThermalStatusLevel
import kotlin.math.max

/**
 * Adaptive Inference Controller.
 * Dynamically adjusts frame inference rate based on:
 * 1. Hand Presence (drops to 6-8 FPS when no hand is in frame)
 * 2. Gesture Candidate State (boosts to 24-26 FPS when stabilizing a gesture)
 * 3. Cooldown / Post-Execution (drops to 9-10 FPS while waiting for cooldown)
 * 4. Configured BatteryMode (Performance, Balanced, Battery Saver, Auto)
 * 5. Hardware Thermal State (automatically reduces rate if device heats up)
 */
class AdaptiveInferenceController {

    private var lastAnalyzedTimeMs = 0L
    private var totalFramesReceived = 0L
    private var totalFramesProcessed = 0L
    private var totalFramesDropped = 0L

    // FPS & Latency computation
    private var fpsWindowStartTime = 0L
    private var fpsWindowCount = 0
    private var currentEffectiveFps = 0

    private var latencySumMs = 0L
    private var latencySamples = 0
    private var currentAvgLatencyMs = 0f

    /**
     * Determines whether the incoming camera frame should be processed or dropped.
     */
    fun shouldProcessFrame(
        isHandPresent: Boolean,
        gestureState: GestureLifecycleState,
        batteryMode: BatteryMode,
        thermalLevel: ThermalStatusLevel = ThermalStatusLevel.NORMAL,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        totalFramesReceived++

        val requiredIntervalMs = calculateTargetIntervalMs(isHandPresent, gestureState, batteryMode, thermalLevel)

        if (now - lastAnalyzedTimeMs >= requiredIntervalMs) {
            lastAnalyzedTimeMs = now
            totalFramesProcessed++

            // Track FPS
            fpsWindowCount++
            if (fpsWindowStartTime == 0L) fpsWindowStartTime = now
            val elapsed = now - fpsWindowStartTime
            if (elapsed >= 1000L) {
                currentEffectiveFps = ((fpsWindowCount * 1000L) / elapsed).toInt()
                fpsWindowCount = 0
                fpsWindowStartTime = now
            }

            return true
        } else {
            totalFramesDropped++
            return false
        }
    }

    /**
     * Records latency of a completed frame analysis.
     */
    fun recordFrameLatency(latencyMs: Long) {
        latencySumMs += latencyMs
        latencySamples++
        if (latencySamples >= 10) {
            currentAvgLatencyMs = latencySumMs.toFloat() / latencySamples
            latencySumMs = 0L
            latencySamples = 0
        }
    }

    private fun calculateTargetIntervalMs(
        isHandPresent: Boolean,
        gestureState: GestureLifecycleState,
        batteryMode: BatteryMode,
        thermalLevel: ThermalStatusLevel
    ): Long {
        // Effective mode if AUTO
        val effectiveMode = if (batteryMode == BatteryMode.AUTO) {
            if (thermalLevel.isThrottled) BatteryMode.BATTERY_SAVER else BatteryMode.BALANCED
        } else {
            batteryMode
        }

        // Base interval by presence & gesture state
        val baseIntervalMs = when {
            !isHandPresent -> 145L // ~7 FPS when no hand in view
            gestureState == GestureLifecycleState.CANDIDATE || gestureState == GestureLifecycleState.STABLE -> 40L // ~25 FPS when tracking a candidate
            gestureState == GestureLifecycleState.EXECUTED || gestureState == GestureLifecycleState.COOLDOWN -> 115L // ~9 FPS during cooldown
            else -> 55L // ~18 FPS baseline
        }

        // Mode multiplier
        val modeMultiplier = when (effectiveMode) {
            BatteryMode.PERFORMANCE -> 0.75f
            BatteryMode.BALANCED -> 1.0f
            BatteryMode.BATTERY_SAVER -> 1.60f
            BatteryMode.AUTO -> 1.0f
        }

        // Thermal throttle penalty
        val thermalMultiplier = if (thermalLevel.isThrottled) 1.50f else 1.0f

        return (baseIntervalMs * modeMultiplier * thermalMultiplier).toLong().coerceAtLeast(28L)
    }

    fun getEffectiveFps(): Int = currentEffectiveFps

    fun getAvgLatencyMs(): Float = currentAvgLatencyMs

    fun getDroppedFrames(): Long = totalFramesDropped

    fun getProcessedFrames(): Long = totalFramesProcessed

    fun reset() {
        lastAnalyzedTimeMs = 0L
        fpsWindowStartTime = 0L
        fpsWindowCount = 0
        currentEffectiveFps = 0
        latencySumMs = 0L
        latencySamples = 0
        currentAvgLatencyMs = 0f
    }
}
