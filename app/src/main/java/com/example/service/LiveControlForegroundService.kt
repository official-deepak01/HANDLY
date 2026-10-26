package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.MainActivity
import com.example.R
import com.example.action.ActionEngine
import com.example.action.FeedbackController
import com.example.data.DefaultProfiles
import com.example.data.PreferencesManager
import com.example.model.ActionHistoryItem
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.BatteryMode
import com.example.model.ControlMode
import com.example.model.Cooldown
import com.example.model.CursorCalibrationData
import com.example.model.CursorInteractionType
import com.example.model.DebugStats
import com.example.model.ExecutionFeedback
import com.example.model.GestureConfirmation
import com.example.model.GestureLifecycleState
import com.example.model.GestureProfile
import com.example.model.GestureType
import com.example.model.HandPreference
import com.example.model.PinchBehavior
import com.example.model.Sensitivity
import com.example.vision.AirCursorEngine
import com.example.vision.GestureCalibrationManager
import com.example.vision.GestureClassifier
import com.example.vision.HandData
import com.example.vision.HandLandmarkerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveControlForegroundService : Service(), LifecycleOwner {

    companion object {
        private const val TAG = "LiveControlService"
        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"

        private const val CHANNEL_ID = "gesture_live_control_channel"
        private const val NOTIFICATION_ID = 2001

        fun startService(context: Context) {
            val intent = Intent(context, LiveControlForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LiveControlForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var prefs: PreferencesManager
    private lateinit var actionEngine: ActionEngine
    private lateinit var feedbackController: FeedbackController
    private lateinit var calibrationManager: GestureCalibrationManager
    private lateinit var classifier: GestureClassifier
    private lateinit var airCursorEngine: AirCursorEngine
    private lateinit var floatingOverlayManager: FloatingOverlayManager
    private var landmarkerHelper: HandLandmarkerHelper? = null

    // Real-time configurations cached from DataStore
    private var currentSensitivity = Sensitivity.MEDIUM
    private var currentCooldown = Cooldown.MEDIUM
    private var currentConfirmation = GestureConfirmation.NORMAL
    private var currentHandPreference = HandPreference.AUTO
    private var isHapticEnabled = true
    private var isAutoProfileSwitchEnabled = true
    private var allProfiles: List<GestureProfile> = DefaultProfiles.getAllBuiltInProfiles()
    private var activeProfile: GestureProfile = DefaultProfiles.GENERAL_PHONE

    // Air Cursor & Power configs
    private var currentControlMode = ControlMode.GESTURE
    private var currentBatteryMode = BatteryMode.BALANCED
    private var currentPinchBehavior = PinchBehavior.CURSOR_CLICK
    private var cursorSensitivity = 1.2f
    private var cursorSmoothing = 0.65f
    private var cursorAcceleration = true
    private var cursorDeadZone = 0.012f
    private var cursorClickThreshold = 0.07f
    private var cursorClickCooldown = 380L
    private var isDwellClickEnabled = false
    private var dwellDurationMs = 1200L
    private var isFloatingOverlayEnabled = true
    private var isHistoryEnabled = true
    private var cursorCalibration = CursorCalibrationData.DEFAULT

    // Performance & FPS tracking
    private var lastAnalyzedFrameTime = 0L
    private var minFrameIntervalMs = 45L // default balanced
    private var frameCount = 0
    private var fpsCalculationStartTime = 0L
    private var currentFps = 0
    private var droppedFrames = 0L

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        prefs = PreferencesManager(applicationContext)
        actionEngine = ActionEngine(applicationContext)
        feedbackController = FeedbackController(applicationContext)
        calibrationManager = GestureCalibrationManager(applicationContext)
        classifier = GestureClassifier(calibrationManager)
        airCursorEngine = AirCursorEngine(applicationContext)
        floatingOverlayManager = FloatingOverlayManager(applicationContext)

        createNotificationChannel()
        observeSettings()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand: action = $action")

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification("Gesture Control is active", "Control mode: ${currentControlMode.displayName}")
                LiveControlState.isRunning.value = true
                if (isFloatingOverlayEnabled) {
                    floatingOverlayManager.updateMode(currentControlMode)
                }
                initializeVisionAndCamera()
            }
            ACTION_STOP -> {
                shutdownService()
            }
        }

        return START_NOT_STICKY
    }

    private fun observeSettings() {
        serviceScope.launch {
            prefs.sensitivity.collect { currentSensitivity = it }
        }
        serviceScope.launch {
            prefs.cooldown.collect { currentCooldown = it }
        }
        serviceScope.launch {
            prefs.gestureConfirmation.collect { currentConfirmation = it }
        }
        serviceScope.launch {
            prefs.handPreference.collect { currentHandPreference = it }
        }
        serviceScope.launch {
            prefs.isHapticFeedbackEnabled.collect { isHapticEnabled = it }
        }
        serviceScope.launch {
            prefs.isAutoProfileSwitchEnabled.collect { isAutoProfileSwitchEnabled = it }
        }
        serviceScope.launch {
            prefs.storedProfiles.collect { profiles ->
                allProfiles = profiles
                updateActiveProfileFromPackage(AccessibilityController.foregroundPackage.value)
            }
        }
        serviceScope.launch {
            AccessibilityController.foregroundPackage.collect { pkg ->
                updateActiveProfileFromPackage(pkg)
            }
        }
        serviceScope.launch {
            prefs.controlMode.collect { mode ->
                currentControlMode = mode
                LiveControlState.controlMode.value = mode
                if (isFloatingOverlayEnabled) {
                    floatingOverlayManager.updateMode(mode)
                }
            }
        }
        serviceScope.launch {
            prefs.batteryMode.collect { mode ->
                currentBatteryMode = mode
                LiveControlState.batteryMode.value = mode
                minFrameIntervalMs = mode.frameIntervalMs
            }
        }
        serviceScope.launch {
            prefs.pinchBehavior.collect { currentPinchBehavior = it }
        }
        serviceScope.launch {
            prefs.cursorSensitivity.collect { cursorSensitivity = it }
        }
        serviceScope.launch {
            prefs.cursorSmoothing.collect { cursorSmoothing = it }
        }
        serviceScope.launch {
            prefs.cursorAcceleration.collect { cursorAcceleration = it }
        }
        serviceScope.launch {
            prefs.cursorDeadZone.collect { cursorDeadZone = it }
        }
        serviceScope.launch {
            prefs.cursorClickThreshold.collect { cursorClickThreshold = it }
        }
        serviceScope.launch {
            prefs.cursorClickCooldown.collect { cursorClickCooldown = it }
        }
        serviceScope.launch {
            prefs.isDwellClickEnabled.collect { isDwellClickEnabled = it }
        }
        serviceScope.launch {
            prefs.dwellDurationMs.collect { dwellDurationMs = it }
        }
        serviceScope.launch {
            prefs.isFloatingOverlayEnabled.collect { enabled ->
                isFloatingOverlayEnabled = enabled
                if (enabled) floatingOverlayManager.updateMode(currentControlMode)
                else floatingOverlayManager.destroy()
            }
        }
        serviceScope.launch {
            prefs.isHistoryEnabled.collect { isHistoryEnabled = it }
        }
        serviceScope.launch {
            prefs.cursorCalibration.collect { cursorCalibration = it }
        }
    }

    private fun updateActiveProfileFromPackage(pkg: String?) {
        if (!isAutoProfileSwitchEnabled || pkg.isNullOrBlank()) return
        LiveControlState.foregroundPackage.value = pkg
        val matching = allProfiles.find { it.packageName == pkg }
        if (matching != null && matching.id != activeProfile.id) {
            activeProfile = matching
            LiveControlState.activeProfile.value = matching
            updateNotification("Gesture Control is active", "Profile: ${matching.name}")
            Log.i(TAG, "Auto-switched active profile to: ${matching.name} for $pkg")
        }
    }

    private fun initializeVisionAndCamera() {
        if (landmarkerHelper == null) {
            landmarkerHelper = HandLandmarkerHelper(
                context = applicationContext,
                onResult = { hand ->
                    onHandDetected(hand)
                },
                onError = { error ->
                    Log.e(TAG, "Landmarker error in service: $error")
                }
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = if (cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    try {
                        val now = System.currentTimeMillis()
                        if (now - lastAnalyzedFrameTime >= minFrameIntervalMs) {
                            lastAnalyzedFrameTime = now
                            landmarkerHelper?.detectLiveStream(imageProxy)
                        } else {
                            droppedFrames++
                            imageProxy.close()
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error in image analyzer", t)
                        try {
                            imageProxy.close()
                        } catch (_: Throwable) {
                        }
                    }
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalysis
                )
                Log.i(TAG, "Camera bound to LiveControlForegroundService successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera in LiveControlForegroundService: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onHandDetected(hand: HandData) {
        val startTime = System.currentTimeMillis()

        // Calculate FPS
        frameCount++
        if (fpsCalculationStartTime == 0L) fpsCalculationStartTime = startTime
        val elapsedSec = (startTime - fpsCalculationStartTime) / 1000.0
        if (elapsedSec >= 1.0) {
            currentFps = (frameCount / elapsedSec).toInt()
            frameCount = 0
            fpsCalculationStartTime = startTime
        }

        LiveControlState.currentHandData.value = hand
        LiveControlState.recognitionConfidence.value = hand.confidence

        val dm = Resources.getSystem().displayMetrics
        val screenWidth = dm.widthPixels.toFloat()
        val screenHeight = dm.heightPixels.toFloat()

        var executedActionName = "None"
        var executionOutcomeResult = "None"

        // Handle Air Cursor Mode
        if (currentControlMode == ControlMode.AIR_CURSOR || currentControlMode == ControlMode.HYBRID) {
            val cursorState = airCursorEngine.processFrame(
                hand = hand,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                sensitivity = cursorSensitivity,
                smoothing = cursorSmoothing,
                acceleration = cursorAcceleration,
                deadZone = cursorDeadZone,
                clickThreshold = cursorClickThreshold,
                clickCooldown = cursorClickCooldown,
                dwellEnabled = isDwellClickEnabled,
                dwellDurationMs = dwellDurationMs,
                pinchBehavior = currentPinchBehavior,
                calibration = cursorCalibration,
                isSafeTestMode = false
            )
            LiveControlState.cursorState.value = cursorState

            if (isFloatingOverlayEnabled) {
                val isClicking = cursorState.interaction in listOf(
                    CursorInteractionType.CLICK,
                    CursorInteractionType.DOUBLE_CLICK,
                    CursorInteractionType.LONG_PRESS,
                    CursorInteractionType.DWELL_CLICK
                )
                floatingOverlayManager.updateCursor(
                    screenX = cursorState.screenX,
                    screenY = cursorState.screenY,
                    isPinching = cursorState.isPinching,
                    isClicking = isClicking
                )
            }

            if (cursorState.interaction in listOf(CursorInteractionType.CLICK, CursorInteractionType.DOUBLE_CLICK, CursorInteractionType.LONG_PRESS, CursorInteractionType.DWELL_CLICK)) {
                feedbackController.triggerHaptic(ActionResult.SUCCESS, isHapticEnabled)
                executedActionName = "Cursor ${cursorState.interaction.name}"
                executionOutcomeResult = "SUCCESS"
                recordHistoryItem(
                    gesture = GestureType.PINCH,
                    action = ActionType.TAP,
                    result = ActionResult.SUCCESS,
                    error = null
                )
            }
        }

        // Handle Gesture Mode (in GESTURE mode or HYBRID when not actively pointing/pinching)
        if (currentControlMode == ControlMode.GESTURE || (currentControlMode == ControlMode.HYBRID && !LiveControlState.cursorState.value.isPinching)) {
            val result = classifier.processHand(
                hand = hand,
                sensitivity = currentSensitivity,
                cooldown = currentCooldown,
                confirmation = currentConfirmation,
                handPreference = currentHandPreference,
                isControlActive = true
            )

            LiveControlState.gestureState.value = result.state

            // Safety Deactivation
            if (result.isSafetyDeactivationTrigger) {
                feedbackController.triggerHaptic(ActionResult.SUCCESS, isHapticEnabled)
                shutdownService()
                return
            }

            val gesture = result.gesture
            if (gesture != null) {
                LiveControlState.lastRecognizedGesture.value = gesture

                if (isFloatingOverlayEnabled && currentControlMode == ControlMode.GESTURE) {
                    floatingOverlayManager.updateStatus(gesture.displayName, activeProfile.name)
                }

                // Resolve action mapping from active profile
                val mapping = activeProfile.mappings[gesture]
                val action = if (mapping != null && mapping.isEnabled) mapping.action else ActionType.NONE
                LiveControlState.lastExecutedAction.value = action

                if (result.state == GestureLifecycleState.EXECUTED) {
                    if (action != ActionType.NONE) {
                        val outcome = actionEngine.executeAction(action)
                        LiveControlState.lastExecutionResult.value = outcome.result
                        LiveControlState.lastError.value = outcome.errorMessage
                        feedbackController.triggerHaptic(outcome.result, isHapticEnabled)

                        executedActionName = action.displayName
                        executionOutcomeResult = outcome.result.name

                        // Record to history
                        recordHistoryItem(
                            gesture = gesture,
                            action = action,
                            result = outcome.result,
                            error = outcome.errorMessage
                        )

                        // Update notification text
                        val actionDesc = if (outcome.result == ActionResult.SUCCESS) action.displayName else "Failed: ${action.displayName}"
                        updateNotification("Gesture Control is active", "${activeProfile.name}: $actionDesc")

                        LiveControlState.visualFeedback.value = ExecutionFeedback(
                            gesture = gesture,
                            action = action,
                            result = outcome.result,
                            message = actionDesc
                        )
                    }
                }
            }
        }

        val procTime = System.currentTimeMillis() - startTime

        // Update real-time debug diagnostics
        LiveControlState.debugStats.value = DebugStats(
            cameraFps = currentFps,
            handConfidence = hand.confidence,
            landmarkCount = hand.landmarks.size,
            gestureConfidence = LiveControlState.recognitionConfidence.value,
            gestureStability = 0.9f,
            currentHand = if (hand.isPresent) "Hand Detected" else "None",
            currentProfile = activeProfile.name,
            isAccessibilityConnected = AccessibilityController.isServiceConnected(),
            lastAction = executedActionName,
            lastActionResult = executionOutcomeResult,
            frameProcessingTimeMs = procTime,
            droppedFramesCount = droppedFrames
        )
    }

    private fun recordHistoryItem(
        gesture: GestureType?,
        action: ActionType,
        result: ActionResult,
        error: String?
    ) {
        if (!isHistoryEnabled) return
        val item = ActionHistoryItem(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            gesture = gesture,
            action = action,
            result = result,
            packageName = AccessibilityController.foregroundPackage.value,
            profileName = activeProfile.name,
            errorMessage = error
        )
        serviceScope.launch {
            prefs.addActionHistoryItem(item)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Gesture Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing status when Gesture Control is actively processing camera gestures."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LiveControlForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = R.mipmap.ic_launcher

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundWithNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun shutdownService() {
        Log.i(TAG, "Shutting down LiveControlForegroundService")
        try {
            floatingOverlayManager.destroy()
            cameraProvider?.unbindAll()
            cameraExecutor?.shutdown()
            landmarkerHelper?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during service shutdown: ${e.message}", e)
        }

        LiveControlState.isRunning.value = false
        LiveControlState.reset()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        shutdownService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
