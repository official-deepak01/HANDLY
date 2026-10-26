package com.example.model

enum class GestureType(val displayName: String, val isDynamic: Boolean, val description: String) {
    OPEN_PALM("Open Palm", false, "Hold for 1s to activate gesture control (Activation gesture)"),
    FIST("Fist", false, "Hold for 1s to deactivate control, or tap for Back action"),
    THUMBS_UP("Thumbs Up", false, "Thumb extended upward with fingers closed"),
    THUMBS_DOWN("Thumbs Down", false, "Thumb pointing downward with fingers closed"),
    INDEX_POINT("Index Point", false, "Index finger pointing forward with other fingers curled"),
    TWO_FINGERS("Two Fingers", false, "Index and middle fingers extended (V / Peace sign)"),
    PINCH("Pinch", false, "Thumb and index tips touching or close together"),
    OK_SIGN("OK Sign", false, "Thumb and index tips touching to form a circle with other fingers extended"),
    SWIPE_UP("Swipe Up", true, "Quick upward motion of open hand"),
    SWIPE_DOWN("Swipe Down", true, "Quick downward motion of open hand"),
    SWIPE_LEFT("Swipe Left", true, "Quick motion of hand from right to left"),
    SWIPE_RIGHT("Swipe Right", true, "Quick motion of hand from left to right")
}

enum class ActionCategory(val displayName: String) {
    NONE("None"),
    NAVIGATION("Navigation"),
    SCROLLING("Scrolling"),
    TOUCH("Touch"),
    MEDIA("Media"),
    VOLUME("Volume"),
    CAMERA("Camera"),
    SYSTEM("System")
}

enum class ActionType(val displayName: String, val category: ActionCategory, val description: String) {
    NONE("No Action", ActionCategory.NONE, "Do nothing when gesture is recognized"),
    BACK("Back", ActionCategory.NAVIGATION, "System navigation back (GLOBAL_ACTION_BACK)"),
    HOME("Home", ActionCategory.NAVIGATION, "Go to home screen (GLOBAL_ACTION_HOME)"),
    RECENTS("Recent Apps", ActionCategory.NAVIGATION, "Open app switcher (GLOBAL_ACTION_RECENTS)"),
    SCROLL_FORWARD("Scroll Forward", ActionCategory.SCROLLING, "Scroll down/forward in reading content"),
    SCROLL_BACKWARD("Scroll Backward", ActionCategory.SCROLLING, "Scroll up/backward in reading content"),
    SCROLL_LEFT("Scroll Left", ActionCategory.SCROLLING, "Horizontal scroll towards left"),
    SCROLL_RIGHT("Scroll Right", ActionCategory.SCROLLING, "Horizontal scroll towards right"),
    TAP("Tap", ActionCategory.TOUCH, "Perform a screen tap at the target location"),
    DOUBLE_TAP("Double Tap", ActionCategory.TOUCH, "Perform double tap at target location"),
    LONG_PRESS("Long Press", ActionCategory.TOUCH, "Perform sustained long press at target location"),
    SWIPE_LEFT("Swipe Left", ActionCategory.TOUCH, "Perform swipe left gesture on screen"),
    SWIPE_RIGHT("Swipe Right", ActionCategory.TOUCH, "Perform swipe right gesture on screen"),
    TOUCH_SWIPE_UP("Swipe Up Screen", ActionCategory.TOUCH, "Simulate swipe up gesture on screen"),
    TOUCH_SWIPE_DOWN("Swipe Down Screen", ActionCategory.TOUCH, "Simulate swipe down gesture on screen"),
    TOUCH_SWIPE_LEFT("Swipe Left Screen", ActionCategory.TOUCH, "Simulate swipe left gesture on screen"),
    TOUCH_SWIPE_RIGHT("Swipe Right Screen", ActionCategory.TOUCH, "Simulate swipe right gesture on screen"),
    PLAY_PAUSE("Play / Pause", ActionCategory.MEDIA, "Toggle media playback (Key / AudioManager)"),
    NEXT_TRACK("Next Track", ActionCategory.MEDIA, "Skip to next media item"),
    PREV_TRACK("Previous Track", ActionCategory.MEDIA, "Return to previous media item"),
    VOLUME_UP("Volume Up", ActionCategory.VOLUME, "Raise audio volume by one step"),
    VOLUME_DOWN("Volume Down", ActionCategory.VOLUME, "Lower audio volume by one step"),
    MUTE("Mute Audio", ActionCategory.VOLUME, "Toggle audio mute / silent"),
    CAMERA_SHUTTER("Camera Shutter", ActionCategory.CAMERA, "Trigger camera capture via volume shutter"),
    NOTIFICATIONS("Notifications", ActionCategory.SYSTEM, "Pull down notification shade (GLOBAL_ACTION_NOTIFICATIONS)"),
    QUICK_SETTINGS("Quick Settings", ActionCategory.SYSTEM, "Open quick settings panel (GLOBAL_ACTION_QUICK_SETTINGS)")
}

enum class ActionResult {
    SUCCESS,
    FAILED,
    UNSUPPORTED,
    SERVICE_DISABLED,
    NOT_ACTIVE,
    COOLDOWN
}

data class ActionExecutionOutcome(
    val result: ActionResult,
    val errorMessage: String? = null
)

enum class HandDistance(val label: String) {
    NO_HAND("NO HAND"),
    TOO_FAR("TOO FAR"),
    GOOD("GOOD"),
    TOO_CLOSE("TOO CLOSE")
}

data class NormalizedPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

data class HandData(
    val isPresent: Boolean,
    val landmarks: List<NormalizedPoint>,
    val confidence: Float,
    val centerX: Float,
    val centerY: Float,
    val handSize: Float,
    val distance: HandDistance
) {
    companion object {
        val EMPTY = HandData(
            isPresent = false,
            landmarks = emptyList(),
            confidence = 0f,
            centerX = 0.5f,
            centerY = 0.5f,
            handSize = 0f,
            distance = HandDistance.NO_HAND
        )
    }
}

enum class GestureLifecycleState {
    READY,
    DETECTING,
    DETECTED,
    CANDIDATE,
    STABLE,
    CONFIRMED,
    EXECUTED,
    COOLDOWN
}

enum class Sensitivity(val displayName: String, val confidenceThreshold: Float, val minHoldMs: Long) {
    LOW("Low", 0.80f, 600L),
    MEDIUM("Medium", 0.65f, 400L),
    HIGH("High", 0.50f, 250L)
}

enum class Cooldown(val displayName: String, val durationMs: Long) {
    SHORT("Short (400ms)", 400L),
    MEDIUM("Medium (700ms)", 700L),
    LONG("Long (1100ms)", 1100L)
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class ProfileMapping(
    val gesture: GestureType,
    val action: ActionType,
    val isEnabled: Boolean = true
)

data class GestureProfile(
    val id: String,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean,
    val packageName: String? = null,
    val mappings: Map<GestureType, ProfileMapping>
)

data class ExecutionFeedback(
    val gesture: GestureType,
    val action: ActionType,
    val result: ActionResult,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = ""
)

enum class HandPreference(val displayName: String) {
    AUTO("Auto Detect"),
    RIGHT("Right Hand"),
    LEFT("Left Hand")
}

enum class GestureConfirmation(val displayName: String, val minHoldMs: Long, val stabilityFrames: Int) {
    OFF("Off (Immediate)", 120L, 1),
    NORMAL("Normal (Balanced)", 350L, 4),
    STRICT("Strict (High Stability)", 600L, 7)
}

data class GestureCalibrationData(
    val gesture: GestureType,
    val sampleCount: Int,
    val meanFeatures: List<Float>,
    val varianceFeatures: List<Float>,
    val averageConfidence: Float,
    val qualityPercentage: Int,
    val isTrained: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class CalibrationOverallStatus(val displayName: String) {
    UNCALIBRATED("Uncalibrated"),
    FAIR("Fair"),
    GOOD("Good"),
    EXCELLENT("Excellent")
}
