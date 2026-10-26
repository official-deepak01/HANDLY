package com.example.model

enum class ControlMode(val displayName: String, val description: String) {
    GESTURE("Gesture Control", "Control apps with distinct hand gestures (swipes, fist, palm, etc.)"),
    AIR_CURSOR("Air Cursor", "Point with your index finger to move an on-screen cursor, pinch to click"),
    HYBRID("Hybrid Mode", "Use Air Cursor when pointing forward, and gestures when making whole-hand motions")
}

enum class PinchBehavior(val displayName: String) {
    CURSOR_CLICK("Cursor Click (Tap at pointer)"),
    GESTURE_TAP("Gesture Action (Trigger profile action)"),
    DISABLED("Disabled")
}

enum class BatteryMode(val displayName: String, val targetFps: Int, val frameIntervalMs: Long, val description: String) {
    BATTERY_SAVER("Battery Saver", 12, 85L, "Power-saving (~12 FPS). Lowest CPU and thermal impact for maximum battery life."),
    BALANCED("Balanced", 18, 55L, "Recommended default (~18 FPS). Smooth recognition with cool device thermals."),
    PERFORMANCE("Performance", 28, 35L, "Full recognition accuracy (~28 FPS). High performance mode."),
    AUTO("Auto", 18, 55L, "Automatically adapts processing rate based on hand presence, gesture activity, and device thermals.")
}

data class CursorPoint(
    val x: Float,
    val y: Float
)

data class CursorCalibrationData(
    val center: CursorPoint = CursorPoint(0.5f, 0.5f),
    val topLeft: CursorPoint = CursorPoint(0.2f, 0.2f),
    val topRight: CursorPoint = CursorPoint(0.8f, 0.2f),
    val bottomLeft: CursorPoint = CursorPoint(0.2f, 0.8f),
    val bottomRight: CursorPoint = CursorPoint(0.8f, 0.8f),
    val isCalibrated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT = CursorCalibrationData()
    }
}

enum class CursorInteractionType {
    NONE,
    MOVE,
    CLICK,
    DOUBLE_CLICK,
    LONG_PRESS,
    DRAG_START,
    DRAG_MOVE,
    DRAG_END,
    DWELL_CLICK
}

data class CursorState(
    val screenX: Float = 0f,
    val screenY: Float = 0f,
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val isPointing: Boolean = false,
    val isPinching: Boolean = false,
    val pinchDistance: Float = 1.0f,
    val interaction: CursorInteractionType = CursorInteractionType.NONE,
    val dwellProgress: Float = 0f,
    val lastActionTime: Long = 0L
) {
    companion object {
        val IDLE = CursorState()
    }
}

data class ActionHistoryItem(
    val id: String,
    val timestamp: Long,
    val gesture: GestureType?,
    val action: ActionType,
    val result: ActionResult,
    val packageName: String?,
    val profileName: String,
    val errorMessage: String? = null
)

data class DebugStats(
    val cameraFps: Int = 0,
    val handConfidence: Float = 0f,
    val landmarkCount: Int = 0,
    val gestureConfidence: Float = 0f,
    val gestureStability: Float = 0f,
    val currentHand: String = "None",
    val currentProfile: String = "General Phone",
    val isAccessibilityConnected: Boolean = false,
    val lastAction: String = "None",
    val lastActionResult: String = "None",
    val frameProcessingTimeMs: Long = 0L,
    val droppedFramesCount: Long = 0L
)
