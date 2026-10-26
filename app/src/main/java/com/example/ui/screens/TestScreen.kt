package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.GestureLifecycleState
import com.example.model.GestureType
import com.example.model.HandDistance
import com.example.service.AccessibilityController
import com.example.ui.components.CameraPreviewView
import com.example.ui.theme.HandlyCyan
import com.example.ui.theme.HandlyElevated
import com.example.ui.theme.HandlyError
import com.example.ui.theme.HandlySuccess
import com.example.ui.theme.HandlySurface
import com.example.ui.theme.HandlyTextPrimary
import com.example.ui.theme.HandlyTextSecondary
import com.example.ui.theme.HandlyViolet
import com.example.ui.theme.HandlyVioletBright
import com.example.ui.theme.HandlyWarning
import com.example.viewmodel.GestureControlViewModel

@Composable
fun TestScreen(
    viewModel: GestureControlViewModel,
    initialTargetGesture: GestureType? = null
) {
    val context = LocalContext.current

    val isTestMode by viewModel.isTestMode.collectAsState()
    val isControlActive by viewModel.isControlActive.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val handData by viewModel.currentHandData.collectAsState()
    val gesture by viewModel.lastRecognizedGesture.collectAsState()
    val confidence by viewModel.recognitionConfidence.collectAsState()
    val gestureState by viewModel.gestureState.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val lastAction by viewModel.lastExecutedAction.collectAsState()
    val lastResult by viewModel.lastExecutionResult.collectAsState()
    val lastError by viewModel.lastError.collectAsState()

    var selectedTargetGesture by remember { mutableStateOf<GestureType?>(initialTargetGesture) }
    val gestureDetectionCounts = remember { mutableStateMapOf<GestureType, Int>() }

    // Count successful detections when CONFIRMED or EXECUTED
    LaunchedEffect(gesture, gestureState) {
        if (gesture != null && (gestureState == GestureLifecycleState.CONFIRMED || gestureState == GestureLifecycleState.EXECUTED)) {
            val current = gestureDetectionCounts[gesture!!] ?: 0
            gestureDetectionCounts[gesture!!] = current + 1
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        viewModel.setCameraReady(isGranted)
    }

    val scrollState = rememberScrollState()
    val targetChipScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. HEADER (Section 9: Rename/rebrand Live Control)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Control",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = HandlyTextPrimary
                )
                Text(
                    text = "Your hands are the remote.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HandlyVioletBright,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Live Control status pill
            val statusColor = if (isControlActive) HandlySuccess else HandlyTextSecondary
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isControlActive) HandlySuccess.copy(alpha = 0.18f) else HandlyElevated)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isControlActive) "● Active" else "○ Standby",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        // ==========================================
        // 2. LIVE CONTROL TELEMETRY CARD (Section 9)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isControlActive) HandlySuccess.copy(alpha = 0.5f) else HandlyViolet.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Title + Hand presence
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE CONTROL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = HandlyVioletBright,
                        letterSpacing = 1.2.sp
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (handData.isPresent) HandlySuccess.copy(alpha = 0.20f)
                                else HandlyElevated
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (handData.isPresent) HandlySuccess else HandlyTextSecondary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (handData.isPresent) "HAND DETECTED" else "NO HAND DETECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (handData.isPresent) HandlySuccess else HandlyTextSecondary
                        )
                    }
                }

                // Metric Grid: Gesture | Confidence | Action | Result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Gesture", fontSize = 11.sp, color = HandlyTextSecondary)
                        Text(
                            text = if (handData.isPresent && gesture != null) gesture!!.displayName else "None",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = HandlyCyan
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Confidence", fontSize = 11.sp, color = HandlyTextSecondary)
                        val confStr = if (handData.isPresent && confidence > 0f) "${(confidence * 100).toInt()}%" else "--"
                        Text(
                            text = confStr,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (confidence >= 0.75f) HandlySuccess else if (confidence >= 0.5f) HandlyWarning else HandlyTextPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(text = "Action", fontSize = 11.sp, color = HandlyTextSecondary)
                        Text(
                            text = lastAction?.displayName ?: "None",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = HandlyVioletBright
                        )
                    }
                }

                // Execution Result & Distance Guidance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HandlyElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Execution Result
                    val execColor = when (lastResult) {
                        ActionResult.SUCCESS -> HandlySuccess
                        ActionResult.FAILED, ActionResult.SERVICE_DISABLED -> HandlyError
                        else -> HandlyCyan
                    }
                    val execText = when (lastResult) {
                        ActionResult.SUCCESS -> "✓ Executed"
                        ActionResult.FAILED -> "✗ Failed"
                        ActionResult.SERVICE_DISABLED -> "Accessibility required"
                        ActionResult.UNSUPPORTED -> "Unsupported"
                        null -> "Ready"
                        else -> lastResult!!.name
                    }
                    Text(
                        text = execText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = execColor
                    )

                    // Distance Guidance
                    val (distLabel, distColor) = when (handData.distance) {
                        HandDistance.GOOD -> "Distance: Good (20-50 cm)" to HandlySuccess
                        HandDistance.TOO_CLOSE -> "Move hand back" to HandlyError
                        HandDistance.TOO_FAR -> "Move hand closer" to HandlyWarning
                        HandDistance.NO_HAND -> "Show hand" to HandlyTextSecondary
                    }
                    Text(
                        text = distLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = distColor
                    )
                }
            }
        }

        // Accessibility Service Warning Card (if disconnected)
        if (!isAccessibilityConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HandlyError.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = HandlyError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accessibility Service Disconnected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HandlyError
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Android Accessibility Service must be enabled to execute real phone actions (Home, Back, Recents).",
                        fontSize = 12.sp,
                        color = HandlyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { AccessibilityController.openAccessibilitySettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = HandlyError),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Accessibility Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // 3. LIVE CAMERA PREVIEW & SKELETON
        // ==========================================
        if (hasCameraPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HandlyViolet.copy(alpha = 0.3f))
            ) {
                CameraPreviewView(
                    handData = handData,
                    activeGesture = gesture,
                    onFrame = { frame -> viewModel.processFrame(frame) },
                    onCameraReady = { viewModel.setCameraReady(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = HandlySurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = HandlyCyan,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Camera Permission Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = HandlyTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Grant camera access to start live hand skeleton tracking",
                        fontSize = 12.sp,
                        color = HandlyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = HandlyViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // 4. CONTROL SWITCHES: LIVE CONTROL & SAFE MODE
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Live Control Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Control Execution",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HandlyTextPrimary
                        )
                        Text(
                            text = if (isControlActive) "Active: Camera triggers real actions" else "Inactive: Control paused",
                            fontSize = 11.sp,
                            color = HandlyTextSecondary
                        )
                    }
                    Switch(
                        checked = isControlActive,
                        onCheckedChange = { active ->
                            if (active) viewModel.startControl() else viewModel.stopControl()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HandlySuccess,
                            checkedTrackColor = HandlySuccess.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("live_control_switch")
                    )
                }

                // Safe Test Mode Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Safe Test Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HandlyTextPrimary
                        )
                        Text(
                            text = if (isTestMode) "Actions simulated, phone navigation protected" else "OFF: Executes real system actions",
                            fontSize = 11.sp,
                            color = HandlyTextSecondary
                        )
                    }
                    Switch(
                        checked = isTestMode,
                        onCheckedChange = { viewModel.setTestMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HandlyCyan,
                            checkedTrackColor = HandlyCyan.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("test_mode_switch")
                    )
                }
            }
        }

        // Target Gesture Selector (Filter)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Gesture to Test",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HandlyTextPrimary
                )
                if (selectedTargetGesture != null) {
                    Text(
                        text = "Clear Filter",
                        fontSize = 11.sp,
                        color = HandlyCyan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(HandlyCyan.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(targetChipScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTargetGesture == null,
                    onClick = { selectedTargetGesture = null },
                    label = { Text("Auto-Detect (All)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HandlyCyan.copy(alpha = 0.2f),
                        selectedLabelColor = HandlyCyan
                    ),
                    modifier = Modifier.testTag("target_chip_all")
                )
                GestureType.values().forEach { g ->
                    val isSelected = selectedTargetGesture == g
                    val count = gestureDetectionCounts[g] ?: 0
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTargetGesture = if (isSelected) null else g },
                        label = {
                            Text(
                                if (count > 0) "${g.displayName} ($count)" else g.displayName,
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HandlyViolet.copy(alpha = 0.25f),
                            selectedLabelColor = HandlyVioletBright
                        ),
                        modifier = Modifier.testTag("target_chip_${g.name.lowercase()}")
                    )
                }
            }
        }

        // Live Stability & Coaching Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("coaching_feedback_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Top Row: Lifecycle State Badge + Distance Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stateColor = when (gestureState) {
                        GestureLifecycleState.EXECUTED, GestureLifecycleState.CONFIRMED -> HandlySuccess
                        GestureLifecycleState.STABLE -> HandlyCyan
                        GestureLifecycleState.CANDIDATE, GestureLifecycleState.COOLDOWN -> HandlyWarning
                        else -> HandlyTextSecondary
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "State: ${gestureState.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = stateColor
                        )
                    }

                    val distanceColor = when (handData.distance) {
                        HandDistance.GOOD -> HandlySuccess
                        HandDistance.TOO_CLOSE, HandDistance.TOO_FAR -> HandlyWarning
                        else -> HandlyTextSecondary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(distanceColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Distance: ${handData.distance.label}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = distanceColor
                        )
                    }
                }

                // Confidence Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tracking Confidence",
                            fontSize = 11.sp,
                            color = HandlyTextSecondary
                        )
                        Text(
                            text = "${(confidence * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (confidence >= 0.75f) HandlySuccess else if (confidence >= 0.5f) HandlyWarning else HandlyError
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { confidence },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (confidence >= 0.75f) HandlySuccess else if (confidence >= 0.5f) HandlyWarning else HandlyError,
                        trackColor = HandlyElevated
                    )
                }

                // Coaching Tip
                val coachingTip = computeCoachingTip(
                    handPresent = handData.isPresent,
                    distance = handData.distance,
                    targetGesture = selectedTargetGesture,
                    detectedGesture = gesture,
                    gestureState = gestureState
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HandlyElevated)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = HandlyWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = coachingTip,
                        fontSize = 12.sp,
                        color = HandlyTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun computeCoachingTip(
    handPresent: Boolean,
    distance: HandDistance,
    targetGesture: GestureType?,
    detectedGesture: GestureType?,
    gestureState: GestureLifecycleState
): String {
    if (!handPresent) {
        return "Position your hand 20–50 cm in front of the camera to begin."
    }
    when (distance) {
        HandDistance.TOO_CLOSE -> return "Your hand is too close. Move back slightly for better finger tracking."
        HandDistance.TOO_FAR -> return "Your hand is too far. Bring it closer for accurate landmark detection."
        HandDistance.NO_HAND -> return "Scanning for hand..."
        HandDistance.GOOD -> { /* proceed */ }
    }
    if (targetGesture != null) {
        if (detectedGesture == targetGesture) {
            return when (gestureState) {
                GestureLifecycleState.CONFIRMED, GestureLifecycleState.EXECUTED ->
                    "Excellent! Gesture recognized and executed."
                GestureLifecycleState.STABLE ->
                    "Hold steady for confirmation..."
                else ->
                    "Gesture detected, analyzing stability..."
            }
        } else if (detectedGesture != null) {
            return "Detected ${detectedGesture.displayName}. Adjust your fingers for ${targetGesture.displayName}."
        } else {
            return "Form the ${targetGesture.displayName} pose clearly in front of the camera."
        }
    }
    return when (gestureState) {
        GestureLifecycleState.CONFIRMED, GestureLifecycleState.EXECUTED ->
            "Gesture recognized and action dispatched."
        GestureLifecycleState.STABLE ->
            "Holding steady..."
        GestureLifecycleState.CANDIDATE ->
            "Analyzing movement..."
        else ->
            "Make a clear gesture to trigger an action."
    }
}
