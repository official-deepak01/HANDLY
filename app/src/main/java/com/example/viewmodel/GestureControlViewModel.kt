package com.example.viewmodel

import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.action.ActionEngine
import com.example.action.FeedbackController
import com.example.data.DefaultProfiles
import com.example.data.PreferencesManager
import com.example.model.ActionHistoryItem
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.BatteryMode
import com.example.model.CalibrationOverallStatus
import com.example.model.ControlMode
import com.example.model.Cooldown
import com.example.model.CursorCalibrationData
import com.example.model.CursorState
import com.example.model.DebugStats
import com.example.model.ExecutionFeedback
import com.example.model.GestureCalibrationData
import com.example.model.GestureConfirmation
import com.example.model.GestureLifecycleState
import com.example.model.GestureProfile
import com.example.model.GestureType
import com.example.model.HandDistance
import com.example.model.HandPreference
import com.example.model.PinchBehavior
import com.example.model.ProfileMapping
import com.example.model.Sensitivity
import com.example.model.ThemeMode
import com.example.service.AccessibilityController
import com.example.service.LiveControlForegroundService
import com.example.service.LiveControlState
import com.example.vision.AirCursorEngine
import com.example.vision.GestureCalibrationManager
import com.example.vision.GestureClassifier
import com.example.vision.HandData
import com.example.vision.HandLandmarkerHelper
import com.example.vision.TrainingProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class GestureControlViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val actionEngine = ActionEngine(application)
    private val feedbackController = FeedbackController(application)
    val calibrationManager = GestureCalibrationManager(application)
    private val classifier = GestureClassifier(calibrationManager)

    // Hand Landmarker for in-app preview / training
    private var landmarkerHelper: HandLandmarkerHelper? = null

    // Core Control States
    private val _isControlActive = MutableStateFlow(false)
    val isControlActive: StateFlow<Boolean> = _isControlActive.asStateFlow()

    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    val isAccessibilityConnected: StateFlow<Boolean> = AccessibilityController.isConnected

    private val _currentHandData = MutableStateFlow(HandData.EMPTY)
    val currentHandData: StateFlow<HandData> = _currentHandData.asStateFlow()

    private val _lastRecognizedGesture = MutableStateFlow<GestureType?>(null)
    val lastRecognizedGesture: StateFlow<GestureType?> = _lastRecognizedGesture.asStateFlow()

    private val _lastExecutedAction = MutableStateFlow<ActionType?>(null)
    val lastExecutedAction: StateFlow<ActionType?> = _lastExecutedAction.asStateFlow()

    private val _lastExecutionResult = MutableStateFlow<ActionResult?>(null)
    val lastExecutionResult: StateFlow<ActionResult?> = _lastExecutionResult.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _gestureState = MutableStateFlow(GestureLifecycleState.READY)
    val gestureState: StateFlow<GestureLifecycleState> = _gestureState.asStateFlow()

    private val _recognitionConfidence = MutableStateFlow(0f)
    val recognitionConfidence: StateFlow<Float> = _recognitionConfidence.asStateFlow()

    private val _visualFeedback = MutableStateFlow<ExecutionFeedback?>(null)
    val visualFeedback: StateFlow<ExecutionFeedback?> = _visualFeedback.asStateFlow()

    private var feedbackDismissJob: Job? = null

    // Training State
    private val _trainingProgress = MutableStateFlow<TrainingProgress?>(null)
    val trainingProgress: StateFlow<TrainingProgress?> = _trainingProgress.asStateFlow()

    val calibrations: StateFlow<Map<GestureType, GestureCalibrationData>> = calibrationManager.calibrations

    // Settings & Persistence
    val isOnboardingCompleted: StateFlow<Boolean> = prefs.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activeProfileId: StateFlow<String> = prefs.activeProfileId
        .stateIn(viewModelScope, SharingStarted.Eagerly, DefaultProfiles.GENERAL_PHONE.id)

    val sensitivity: StateFlow<Sensitivity> = prefs.sensitivity
        .stateIn(viewModelScope, SharingStarted.Eagerly, Sensitivity.MEDIUM)

    val cooldown: StateFlow<Cooldown> = prefs.cooldown
        .stateIn(viewModelScope, SharingStarted.Eagerly, Cooldown.MEDIUM)

    val gestureConfirmation: StateFlow<GestureConfirmation> = prefs.gestureConfirmation
        .stateIn(viewModelScope, SharingStarted.Eagerly, GestureConfirmation.NORMAL)

    val handPreference: StateFlow<HandPreference> = prefs.handPreference
        .stateIn(viewModelScope, SharingStarted.Eagerly, HandPreference.AUTO)

    val isHapticEnabled: StateFlow<Boolean> = prefs.isHapticFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isVisualFeedbackEnabled: StateFlow<Boolean> = prefs.isVisualFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isAutoProfileSwitchEnabled: StateFlow<Boolean> = prefs.isAutoProfileSwitchEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val isProUser: StateFlow<Boolean> = prefs.isProUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val allProfiles: StateFlow<List<GestureProfile>> = prefs.storedProfiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, DefaultProfiles.getAllBuiltInProfiles())

    val activeProfile: StateFlow<GestureProfile> = combine(allProfiles, activeProfileId) { profiles, id ->
        profiles.find { it.id == id } ?: DefaultProfiles.GENERAL_PHONE
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DefaultProfiles.GENERAL_PHONE)

    // Air Cursor & Control Mode States
    val airCursorEngine = AirCursorEngine(application)
    val cursorState: StateFlow<CursorState> = LiveControlState.cursorState
    val debugStats: StateFlow<DebugStats> = LiveControlState.debugStats

    val controlMode: StateFlow<ControlMode> = prefs.controlMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ControlMode.GESTURE)

    val batteryMode: StateFlow<BatteryMode> = prefs.batteryMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, BatteryMode.BALANCED)

    val pinchBehavior: StateFlow<PinchBehavior> = prefs.pinchBehavior
        .stateIn(viewModelScope, SharingStarted.Eagerly, PinchBehavior.CURSOR_CLICK)

    val cursorSensitivity: StateFlow<Float> = prefs.cursorSensitivity
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.2f)

    val cursorSmoothing: StateFlow<Float> = prefs.cursorSmoothing
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.65f)

    val cursorAcceleration: StateFlow<Boolean> = prefs.cursorAcceleration
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val cursorDeadZone: StateFlow<Float> = prefs.cursorDeadZone
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.012f)

    val cursorClickThreshold: StateFlow<Float> = prefs.cursorClickThreshold
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.07f)

    val cursorClickCooldown: StateFlow<Long> = prefs.cursorClickCooldown
        .stateIn(viewModelScope, SharingStarted.Eagerly, 380L)

    val isDwellClickEnabled: StateFlow<Boolean> = prefs.isDwellClickEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val dwellDurationMs: StateFlow<Long> = prefs.dwellDurationMs
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1200L)

    val isFloatingOverlayEnabled: StateFlow<Boolean> = prefs.isFloatingOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isDebugModeEnabled: StateFlow<Boolean> = prefs.isDebugModeEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isHistoryEnabled: StateFlow<Boolean> = prefs.isHistoryEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val cursorCalibration: StateFlow<CursorCalibrationData> = prefs.cursorCalibration
        .stateIn(viewModelScope, SharingStarted.Eagerly, CursorCalibrationData.DEFAULT)

    val actionHistory: StateFlow<List<ActionHistoryItem>> = prefs.actionHistory
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        initLandmarker()
        observeForegroundApp()
        observeLiveControlState()
    }

    private fun initLandmarker() {
        landmarkerHelper = HandLandmarkerHelper(
            context = getApplication(),
            onResult = { hand ->
                _currentHandData.value = hand
                if (_trainingProgress.value != null) {
                    val progress = calibrationManager.feedTrainingSample(hand)
                    _trainingProgress.value = progress
                } else {
                    processHandData(hand)
                }
            },
            onError = { _ ->
                _isCameraReady.value = false
            }
        )
    }

    private fun observeLiveControlState() {
        viewModelScope.launch {
            LiveControlState.isRunning.collect { running ->
                _isControlActive.value = running
            }
        }
        viewModelScope.launch {
            LiveControlState.currentHandData.collect { hand ->
                if (_isControlActive.value) {
                    _currentHandData.value = hand
                }
            }
        }
        viewModelScope.launch {
            LiveControlState.lastRecognizedGesture.collect { g ->
                if (_isControlActive.value && g != null) {
                    _lastRecognizedGesture.value = g
                }
            }
        }
        viewModelScope.launch {
            LiveControlState.lastExecutedAction.collect { a ->
                if (_isControlActive.value && a != null) {
                    _lastExecutedAction.value = a
                }
            }
        }
        viewModelScope.launch {
            LiveControlState.lastExecutionResult.collect { r ->
                if (_isControlActive.value && r != null) {
                    _lastExecutionResult.value = r
                }
            }
        }
        viewModelScope.launch {
            LiveControlState.lastError.collect { err ->
                if (_isControlActive.value) {
                    _lastError.value = err
                }
            }
        }
        viewModelScope.launch {
            LiveControlState.gestureState.collect { s ->
                if (_isControlActive.value) {
                    _gestureState.value = s
                }
            }
        }
        viewModelScope.launch {
            LiveControlState.recognitionConfidence.collect { c ->
                if (_isControlActive.value) {
                    _recognitionConfidence.value = c
                }
            }
        }
        viewModelScope.launch {
            LiveControlState.visualFeedback.collect { vf ->
                if (_isControlActive.value && vf != null) {
                    _visualFeedback.value = vf
                }
            }
        }
    }

    fun setCameraReady(ready: Boolean) {
        _isCameraReady.value = ready
    }

    fun setTestMode(testMode: Boolean) {
        _isTestMode.value = testMode
    }

    fun startControl() {
        _isControlActive.value = true
        classifier.reset()
        LiveControlForegroundService.startService(getApplication())
    }

    fun stopControl() {
        _isControlActive.value = false
        classifier.reset()
        _currentHandData.value = HandData.EMPTY
        _gestureState.value = GestureLifecycleState.READY
        LiveControlForegroundService.stopService(getApplication())
    }

    fun toggleControl() {
        if (_isControlActive.value) {
            stopControl()
        } else {
            startControl()
        }
    }

    fun processFrame(imageProxy: ImageProxy) {
        val helper = landmarkerHelper
        if (helper == null) {
            try { imageProxy.close() } catch (_: Throwable) {}
            return
        }
        helper.detectLiveStream(imageProxy)
    }

    private fun processHandData(hand: HandData) {
        if (controlMode.value == ControlMode.AIR_CURSOR || controlMode.value == ControlMode.HYBRID || _isTestMode.value) {
            val dm = android.content.res.Resources.getSystem().displayMetrics
            val w = dm.widthPixels.toFloat()
            val h = dm.heightPixels.toFloat()
            val cState = airCursorEngine.processFrame(
                hand = hand,
                screenWidth = w,
                screenHeight = h,
                sensitivity = cursorSensitivity.value,
                smoothing = cursorSmoothing.value,
                acceleration = cursorAcceleration.value,
                deadZone = cursorDeadZone.value,
                clickThreshold = cursorClickThreshold.value,
                clickCooldown = cursorClickCooldown.value,
                dwellEnabled = isDwellClickEnabled.value,
                dwellDurationMs = dwellDurationMs.value,
                pinchBehavior = pinchBehavior.value,
                calibration = cursorCalibration.value,
                isSafeTestMode = _isTestMode.value || !_isControlActive.value
            )
            LiveControlState.cursorState.value = cState
        }

        val result = classifier.processHand(
            hand = hand,
            sensitivity = sensitivity.value,
            cooldown = cooldown.value,
            confirmation = gestureConfirmation.value,
            handPreference = handPreference.value,
            isControlActive = _isControlActive.value
        )

        _gestureState.value = result.state
        _recognitionConfidence.value = result.confidence

        // Handle Safety Triggers
        if (result.isSafetyActivationTrigger) {
            startControl()
            emitFeedback(
                gesture = GestureType.OPEN_PALM,
                action = ActionType.NONE,
                result = ActionResult.SUCCESS,
                message = "Gesture Control Activated"
            )
            feedbackController.triggerHaptic(ActionResult.SUCCESS, isHapticEnabled.value)
            return
        }

        if (result.isSafetyDeactivationTrigger) {
            stopControl()
            emitFeedback(
                gesture = GestureType.FIST,
                action = ActionType.NONE,
                result = ActionResult.SUCCESS,
                message = "Gesture Control Deactivated"
            )
            feedbackController.triggerHaptic(ActionResult.SUCCESS, isHapticEnabled.value)
            return
        }

        val gesture = result.gesture ?: return
        _lastRecognizedGesture.value = gesture

        // Resolve action mapping from active profile
        val mapping = activeProfile.value.mappings[gesture]
        val action = if (mapping != null && mapping.isEnabled) mapping.action else ActionType.NONE
        _lastExecutedAction.value = action

        // Only execute on CONFIRMED/EXECUTED transition
        if (result.state == GestureLifecycleState.EXECUTED) {
            if (_isTestMode.value) {
                _lastExecutionResult.value = ActionResult.SUCCESS
                _lastError.value = null
                emitFeedback(
                    gesture = gesture,
                    action = action,
                    result = ActionResult.SUCCESS,
                    message = "Test Mode: ${action.displayName}"
                )
                feedbackController.triggerHaptic(ActionResult.SUCCESS, isHapticEnabled.value)
            } else if (_isControlActive.value) {
                if (action == ActionType.NONE) {
                    _lastExecutionResult.value = ActionResult.SUCCESS
                    _lastError.value = null
                } else {
                    val outcome = actionEngine.executeAction(action)
                    _lastExecutionResult.value = outcome.result
                    _lastError.value = outcome.errorMessage
                    feedbackController.triggerHaptic(outcome.result, isHapticEnabled.value)
                    emitFeedback(
                        gesture = gesture,
                        action = action,
                        result = outcome.result,
                        message = when (outcome.result) {
                            ActionResult.SUCCESS -> action.displayName
                            ActionResult.SERVICE_DISABLED -> "Accessibility service required"
                            ActionResult.FAILED -> outcome.errorMessage ?: "Action execution failed"
                            ActionResult.UNSUPPORTED -> "Action unsupported on this device"
                            else -> ""
                        }
                    )
                }
            } else {
                _lastExecutionResult.value = ActionResult.NOT_ACTIVE
                _lastError.value = "Live Control is OFF. Tap Start Live Control or hold Open Palm for 1s."
            }
        }
    }

    fun executeManualAction(action: ActionType) {
        _lastExecutedAction.value = action
        val outcome = actionEngine.executeAction(action)
        _lastExecutionResult.value = outcome.result
        _lastError.value = outcome.errorMessage
        feedbackController.triggerHaptic(outcome.result, isHapticEnabled.value)
        emitFeedback(
            gesture = _lastRecognizedGesture.value ?: GestureType.SWIPE_UP,
            action = action,
            result = outcome.result,
            message = when (outcome.result) {
                ActionResult.SUCCESS -> "Action: ${action.displayName}"
                ActionResult.SERVICE_DISABLED -> "Accessibility service required"
                ActionResult.FAILED -> outcome.errorMessage ?: "Action execution failed"
                ActionResult.UNSUPPORTED -> "Action unsupported on this device"
                else -> ""
            }
        )
    }

    // Training Controls
    fun startTrainingGesture(gesture: GestureType) {
        calibrationManager.startTraining(gesture)
        _trainingProgress.value = TrainingProgress(
            gesture = gesture,
            collectedCount = 0,
            targetCount = if (gesture.isDynamic) GestureCalibrationManager.TARGET_SAMPLES_DYNAMIC else GestureCalibrationManager.TARGET_SAMPLES_STATIC,
            currentConfidence = 0f,
            distance = HandDistance.NO_HAND,
            isComplete = false,
            feedbackMessage = "Show your hand in the camera view"
        )
    }

    fun finishTraining(): GestureCalibrationData? {
        val result = calibrationManager.finishTraining()
        _trainingProgress.value = null
        return result
    }

    fun cancelTraining() {
        calibrationManager.cancelTraining()
        _trainingProgress.value = null
    }

    fun resetCalibration(gesture: GestureType) {
        calibrationManager.resetGesture(gesture)
    }

    fun resetAllCalibrations() {
        calibrationManager.resetAll()
    }

    fun setHandPreference(pref: HandPreference) {
        viewModelScope.launch {
            prefs.setHandPreference(pref)
            calibrationManager.setHandPreference(pref)
        }
    }

    fun setGestureConfirmation(conf: GestureConfirmation) {
        viewModelScope.launch {
            prefs.setGestureConfirmation(conf)
        }
    }

    fun getOverallCalibrationQuality(): CalibrationOverallStatus {
        return calibrationManager.getOverallStatus()
    }

    private fun emitFeedback(
        gesture: GestureType,
        action: ActionType,
        result: ActionResult,
        message: String
    ) {
        if (!isVisualFeedbackEnabled.value) return

        feedbackDismissJob?.cancel()
        _visualFeedback.value = ExecutionFeedback(
            gesture = gesture,
            action = action,
            result = result,
            message = message
        )
        feedbackDismissJob = viewModelScope.launch {
            delay(1800L)
            _visualFeedback.value = null
        }
    }

    private fun observeForegroundApp() {
        viewModelScope.launch {
            AccessibilityController.foregroundPackage.collect { pkg ->
                if (pkg != null && isAutoProfileSwitchEnabled.value) {
                    val matching = allProfiles.value.find { it.packageName == pkg }
                    if (matching != null && matching.id != activeProfileId.value) {
                        prefs.setActiveProfileId(matching.id)
                    }
                }
            }
        }
    }

    // Profile & Mapping Management
    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            prefs.setActiveProfileId(profileId)
        }
    }

    fun updateMapping(profileId: String, gesture: GestureType, action: ActionType, isEnabled: Boolean) {
        viewModelScope.launch {
            val currentProfiles = allProfiles.value.map { profile ->
                if (profile.id == profileId) {
                    val updatedMappings = profile.mappings.toMutableMap()
                    updatedMappings[gesture] = ProfileMapping(gesture, action, isEnabled)
                    profile.copy(mappings = updatedMappings)
                } else {
                    profile
                }
            }
            prefs.saveProfiles(currentProfiles)
        }
    }

    fun createCustomProfile(name: String, description: String, packageName: String? = null): String {
        val newId = UUID.randomUUID().toString()
        val defaultMappings = DefaultProfiles.GENERAL_PHONE.mappings.mapValues { (_, mapping) ->
            mapping.copy()
        }
        val newProfile = GestureProfile(
            id = newId,
            name = name,
            description = description,
            isBuiltIn = false,
            packageName = packageName,
            mappings = defaultMappings
        )
        viewModelScope.launch {
            val updated = allProfiles.value + newProfile
            prefs.saveProfiles(updated)
            prefs.setActiveProfileId(newId)
        }
        return newId
    }

    fun deleteCustomProfile(profileId: String) {
        viewModelScope.launch {
            val profileToDelete = allProfiles.value.find { it.id == profileId }
            if (profileToDelete != null && !profileToDelete.isBuiltIn) {
                val updated = allProfiles.value.filter { it.id != profileId }
                prefs.saveProfiles(updated)
                if (activeProfileId.value == profileId) {
                    prefs.setActiveProfileId(DefaultProfiles.GENERAL_PHONE.id)
                }
            }
        }
    }

    fun resetProfileToDefault(profileId: String) {
        viewModelScope.launch {
            val builtIn = DefaultProfiles.getAllBuiltInProfiles().find { it.id == profileId }
            if (builtIn != null) {
                val updated = allProfiles.value.map { if (it.id == profileId) builtIn else it }
                prefs.saveProfiles(updated)
            }
        }
    }

    fun setSensitivity(sens: Sensitivity) {
        viewModelScope.launch { prefs.setSensitivity(sens) }
    }

    fun setCooldown(cd: Cooldown) {
        viewModelScope.launch { prefs.setCooldown(cd) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { prefs.setHapticFeedbackEnabled(enabled) }
    }

    fun setVisualFeedback(enabled: Boolean) {
        viewModelScope.launch { prefs.setVisualFeedbackEnabled(enabled) }
    }

    fun setAutoProfileSwitch(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoProfileSwitchEnabled(enabled) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch { prefs.setOnboardingCompleted(true) }
    }

    fun completeOnboarding() {
        setOnboardingCompleted()
    }

    fun setProUser(isPro: Boolean) {
        viewModelScope.launch { prefs.setProUser(isPro) }
    }

    fun unlockPro() {
        setProUser(true)
    }

    fun updateGestureMapping(
        profileId: String = activeProfileId.value,
        gesture: GestureType,
        action: ActionType,
        isEnabled: Boolean = true
    ) {
        updateMapping(profileId, gesture, action, isEnabled)
    }

    fun duplicateProfile(profileId: String) {
        val original = allProfiles.value.find { it.id == profileId } ?: return
        val newId = UUID.randomUUID().toString()
        val copy = original.copy(
            id = newId,
            name = "${original.name} (Copy)",
            isBuiltIn = false
        )
        viewModelScope.launch {
            val updated = allProfiles.value + copy
            prefs.saveProfiles(updated)
            prefs.setActiveProfileId(newId)
        }
    }

    fun resetProfile(profileId: String) {
        resetProfileToDefault(profileId)
    }

    // Air Cursor & Control Mode Actions
    fun setControlMode(mode: ControlMode) {
        viewModelScope.launch { prefs.setControlMode(mode) }
    }

    fun setPinchBehavior(behavior: PinchBehavior) {
        viewModelScope.launch { prefs.setPinchBehavior(behavior) }
    }

    fun setBatteryMode(mode: BatteryMode) {
        viewModelScope.launch { prefs.setBatteryMode(mode) }
    }

    fun setCursorSensitivity(value: Float) {
        viewModelScope.launch { prefs.setCursorSensitivity(value) }
    }

    fun setCursorSmoothing(value: Float) {
        viewModelScope.launch { prefs.setCursorSmoothing(value) }
    }

    fun setCursorAcceleration(enabled: Boolean) {
        viewModelScope.launch { prefs.setCursorAcceleration(enabled) }
    }

    fun setCursorDeadZone(value: Float) {
        viewModelScope.launch { prefs.setCursorDeadZone(value) }
    }

    fun setCursorClickThreshold(value: Float) {
        viewModelScope.launch { prefs.setCursorClickThreshold(value) }
    }

    fun setCursorClickCooldown(value: Long) {
        viewModelScope.launch { prefs.setCursorClickCooldown(value) }
    }

    fun setDwellClickEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setDwellClickEnabled(enabled) }
    }

    fun setDwellDurationMs(value: Long) {
        viewModelScope.launch { prefs.setDwellDurationMs(value) }
    }

    fun setFloatingOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setFloatingOverlayEnabled(enabled) }
    }

    fun setDebugModeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setDebugModeEnabled(enabled) }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setHistoryEnabled(enabled) }
    }

    fun clearActionHistory() {
        viewModelScope.launch { prefs.clearActionHistory() }
    }

    fun saveCursorCalibration(data: CursorCalibrationData) {
        viewModelScope.launch { prefs.saveCursorCalibration(data) }
    }

    fun resetCursorCalibration() {
        viewModelScope.launch { prefs.resetCursorCalibration() }
    }

    override fun onCleared() {
        super.onCleared()
        landmarkerHelper?.close()
    }
}
