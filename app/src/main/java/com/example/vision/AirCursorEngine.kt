package com.example.vision

import android.content.Context
import android.util.Log
import com.example.model.CursorCalibrationData
import com.example.model.CursorInteractionType
import com.example.model.CursorState
import com.example.model.HandData
import com.example.model.PinchBehavior
import com.example.service.AccessibilityController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class AirCursorEngine(private val context: Context) {

    companion object {
        private const val TAG = "AirCursorEngine"
    }

    private val _cursorState = MutableStateFlow(CursorState.IDLE)
    val cursorState: StateFlow<CursorState> = _cursorState.asStateFlow()

    // Internal smoothed positions
    private var smoothedNormX = 0.5f
    private var smoothedNormY = 0.5f
    private var isInitialized = false

    // Pinch state machine
    private var isPinchHeld = false
    private var pinchStartTime = 0L
    private var pinchStartScreenX = 0f
    private var pinchStartScreenY = 0f
    private var isDragging = false
    private var lastClickTime = 0L

    // Dwell click state machine
    private var dwellAnchorX = 0f
    private var dwellAnchorY = 0f
    private var dwellStartTime = 0L

    fun reset() {
        smoothedNormX = 0.5f
        smoothedNormY = 0.5f
        isInitialized = false
        isPinchHeld = false
        isDragging = false
        pinchStartTime = 0L
        dwellStartTime = 0L
        _cursorState.value = CursorState.IDLE
    }

    fun processFrame(
        hand: HandData,
        screenWidth: Float,
        screenHeight: Float,
        sensitivity: Float = 1.2f,
        smoothing: Float = 0.65f,
        acceleration: Boolean = true,
        deadZone: Float = 0.012f,
        clickThreshold: Float = 0.07f,
        clickCooldown: Long = 380L,
        dwellEnabled: Boolean = false,
        dwellDurationMs: Long = 1200L,
        pinchBehavior: PinchBehavior = PinchBehavior.CURSOR_CLICK,
        calibration: CursorCalibrationData = CursorCalibrationData.DEFAULT,
        isSafeTestMode: Boolean = false
    ): CursorState {
        val now = System.currentTimeMillis()

        if (!hand.isPresent || hand.landmarks.size < 21) {
            isPinchHeld = false
            isDragging = false
            dwellStartTime = 0L
            val idleState = _cursorState.value.copy(
                isPointing = false,
                isPinching = false,
                interaction = CursorInteractionType.NONE,
                dwellProgress = 0f
            )
            _cursorState.value = idleState
            return idleState
        }

        // Landmark 8 = Index fingertip, Landmark 4 = Thumb tip, Landmark 0 = Wrist
        val indexTip = hand.landmarks[8]
        val thumbTip = hand.landmarks[4]
        val wrist = hand.landmarks[0]
        val middleMcp = hand.landmarks[9]

        // Palm scale reference
        val palmScale = hypot(wrist.x - middleMcp.x, wrist.y - middleMcp.y).coerceAtLeast(0.08f)

        // Mirrored X for front camera natural mirror movement
        val rawX = 1f - indexTip.x
        val rawY = indexTip.y

        // Map coordinates using 5-point calibration or default bounding zone
        val mappedX: Float
        val mappedY: Float

        if (calibration.isCalibrated) {
            val minX = min(calibration.topLeft.x, calibration.bottomLeft.x)
            val maxX = max(calibration.topRight.x, calibration.bottomRight.x).coerceAtLeast(minX + 0.1f)
            val minY = min(calibration.topLeft.y, calibration.topRight.y)
            val maxY = max(calibration.bottomLeft.y, calibration.bottomRight.y).coerceAtLeast(minY + 0.1f)

            val uX = ((rawX - minX) / (maxX - minX)).coerceIn(0f, 1f)
            val uY = ((rawY - minY) / (maxY - minY)).coerceIn(0f, 1f)

            // Center-weighted expansion by sensitivity
            mappedX = ((uX - 0.5f) * sensitivity + 0.5f).coerceIn(0f, 1f)
            mappedY = ((uY - 0.5f) * sensitivity + 0.5f).coerceIn(0f, 1f)
        } else {
            // Standard bounding box around center
            val marginX = 0.20f / sensitivity
            val marginY = 0.18f / sensitivity
            val normX = ((rawX - marginX) / (1f - 2f * marginX)).coerceIn(0f, 1f)
            val normY = ((rawY - marginY) / (1f - 2f * marginY)).coerceIn(0f, 1f)
            mappedX = normX
            mappedY = normY
        }

        // Initialize smoothed position on first detection
        if (!isInitialized) {
            smoothedNormX = mappedX
            smoothedNormY = mappedY
            isInitialized = true
        }

        // Dead Zone & Acceleration
        val deltaX = mappedX - smoothedNormX
        val deltaY = mappedY - smoothedNormY
        val distDelta = hypot(deltaX, deltaY)

        if (distDelta > deadZone) {
            val accelFactor = if (acceleration && distDelta > 0.025f) {
                1f + min(distDelta * 2.8f, 1.6f)
            } else {
                1f
            }

            val alpha = (1f - smoothing.coerceIn(0.1f, 0.95f)) * accelFactor
            smoothedNormX = (smoothedNormX + deltaX * alpha).coerceIn(0f, 1f)
            smoothedNormY = (smoothedNormY + deltaY * alpha).coerceIn(0f, 1f)
        }

        val screenX = smoothedNormX * screenWidth
        val screenY = smoothedNormY * screenHeight

        // Check if index finger is extended forward
        val indexPip = hand.landmarks[6]
        val isPointing = indexTip.y < indexPip.y || (hypot(indexTip.x - wrist.x, indexTip.y - wrist.y) > 0.25f)

        // Pinch detection: normalized distance between thumb and index tip
        val rawPinchDist = hypot(thumbTip.x - indexTip.x, thumbTip.y - indexTip.y)
        val pinchNormalizedDist = rawPinchDist / palmScale

        val pinchDownThreshold = clickThreshold
        val pinchUpThreshold = clickThreshold * 1.35f

        var interaction = CursorInteractionType.MOVE
        var isCurrentlyPinching = isPinchHeld

        if (!isPinchHeld && pinchNormalizedDist < pinchDownThreshold) {
            // Pinch Down Event
            isPinchHeld = true
            isCurrentlyPinching = true
            pinchStartTime = now
            pinchStartScreenX = screenX
            pinchStartScreenY = screenY
            isDragging = false
            interaction = CursorInteractionType.MOVE
        } else if (isPinchHeld) {
            if (pinchNormalizedDist > pinchUpThreshold) {
                // Pinch Released Event
                isPinchHeld = false
                isCurrentlyPinching = false
                val pinchDuration = now - pinchStartTime
                val dragDist = hypot(screenX - pinchStartScreenX, screenY - pinchStartScreenY)

                if (isDragging) {
                    interaction = CursorInteractionType.DRAG_END
                    if (!isSafeTestMode && pinchBehavior == PinchBehavior.CURSOR_CLICK) {
                        AccessibilityController.performDrag(pinchStartScreenX, pinchStartScreenY, screenX, screenY, 200L)
                    }
                    isDragging = false
                } else if (dragDist > 65f && pinchDuration > 180L) {
                    // Quick flick drag
                    interaction = CursorInteractionType.DRAG_END
                    if (!isSafeTestMode && pinchBehavior == PinchBehavior.CURSOR_CLICK) {
                        AccessibilityController.performDrag(pinchStartScreenX, pinchStartScreenY, screenX, screenY, 200L)
                    }
                } else if (now - lastClickTime >= clickCooldown) {
                    // Tap or Double Tap
                    val isDouble = (now - lastClickTime < 350L)
                    lastClickTime = now
                    if (isDouble) {
                        interaction = CursorInteractionType.DOUBLE_CLICK
                        if (!isSafeTestMode && pinchBehavior == PinchBehavior.CURSOR_CLICK) {
                            AccessibilityController.performDoubleTap(screenX, screenY)
                        }
                    } else {
                        interaction = CursorInteractionType.CLICK
                        if (!isSafeTestMode && pinchBehavior == PinchBehavior.CURSOR_CLICK) {
                            AccessibilityController.performTap(screenX, screenY)
                        }
                    }
                }
            } else {
                // Pinch Held Ongoing
                val dragDist = hypot(screenX - pinchStartScreenX, screenY - pinchStartScreenY)
                val holdDuration = now - pinchStartTime

                if (dragDist > 55f) {
                    isDragging = true
                    interaction = CursorInteractionType.DRAG_MOVE
                } else if (holdDuration > 550L && !isDragging) {
                    // Long press trigger once
                    interaction = CursorInteractionType.LONG_PRESS
                    if (!isSafeTestMode && pinchBehavior == PinchBehavior.CURSOR_CLICK && (now - lastClickTime >= clickCooldown)) {
                        lastClickTime = now
                        AccessibilityController.performLongPress(screenX, screenY, 600L)
                    }
                }
            }
        }

        // Dwell Click Logic (auto-click when stationary)
        var dwellProgress = 0f
        if (dwellEnabled && !isPinchHeld) {
            val dwellDist = hypot(screenX - dwellAnchorX, screenY - dwellAnchorY)
            if (dwellDist < 35f && dwellStartTime > 0L) {
                val elapsed = now - dwellStartTime
                dwellProgress = (elapsed.toFloat() / dwellDurationMs.toFloat()).coerceIn(0f, 1f)
                if (dwellProgress >= 1f && now - lastClickTime >= clickCooldown) {
                    interaction = CursorInteractionType.DWELL_CLICK
                    lastClickTime = now
                    dwellStartTime = 0L
                    dwellProgress = 0f
                    if (!isSafeTestMode && pinchBehavior == PinchBehavior.CURSOR_CLICK) {
                        AccessibilityController.performTap(screenX, screenY)
                    }
                }
            } else {
                dwellAnchorX = screenX
                dwellAnchorY = screenY
                dwellStartTime = now
                dwellProgress = 0f
            }
        } else {
            dwellStartTime = 0L
            dwellProgress = 0f
        }

        val updatedState = CursorState(
            screenX = screenX,
            screenY = screenY,
            normalizedX = smoothedNormX,
            normalizedY = smoothedNormY,
            isPointing = isPointing,
            isPinching = isCurrentlyPinching,
            pinchDistance = pinchNormalizedDist,
            interaction = interaction,
            dwellProgress = dwellProgress,
            lastActionTime = if (interaction in listOf(CursorInteractionType.CLICK, CursorInteractionType.DOUBLE_CLICK, CursorInteractionType.LONG_PRESS, CursorInteractionType.DWELL_CLICK)) now else _cursorState.value.lastActionTime
        )

        _cursorState.value = updatedState
        return updatedState
    }
}
