package com.example.service

import com.example.data.DefaultProfiles
import com.example.model.ActionExecutionOutcome
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.BatteryMode
import com.example.model.ControlMode
import com.example.model.CursorState
import com.example.model.DebugStats
import com.example.model.ExecutionFeedback
import com.example.model.GestureLifecycleState
import com.example.model.GestureProfile
import com.example.model.GestureType
import com.example.vision.HandData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LiveControlState {
    val isRunning = MutableStateFlow(false)
    val controlMode = MutableStateFlow(ControlMode.GESTURE)
    val batteryMode = MutableStateFlow(BatteryMode.BALANCED)
    val currentHandData = MutableStateFlow(HandData.EMPTY)
    val cursorState = MutableStateFlow(CursorState.IDLE)
    val debugStats = MutableStateFlow(DebugStats())
    val lastRecognizedGesture = MutableStateFlow<GestureType?>(null)
    val lastExecutedAction = MutableStateFlow<ActionType?>(null)
    val lastExecutionResult = MutableStateFlow<ActionResult?>(null)
    val lastError = MutableStateFlow<String?>(null)
    val gestureState = MutableStateFlow(GestureLifecycleState.READY)
    val recognitionConfidence = MutableStateFlow(0f)
    val activeProfile = MutableStateFlow<GestureProfile>(DefaultProfiles.GENERAL_PHONE)
    val visualFeedback = MutableStateFlow<ExecutionFeedback?>(null)
    val foregroundPackage = MutableStateFlow<String?>(null)

    fun reset() {
        currentHandData.value = HandData.EMPTY
        cursorState.value = CursorState.IDLE
        lastRecognizedGesture.value = null
        lastExecutedAction.value = null
        lastExecutionResult.value = null
        lastError.value = null
        gestureState.value = GestureLifecycleState.READY
        recognitionConfidence.value = 0f
    }
}
