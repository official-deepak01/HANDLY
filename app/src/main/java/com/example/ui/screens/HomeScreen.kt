package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ActionResult
import com.example.model.CalibrationOverallStatus
import com.example.service.AccessibilityController
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.HandSkeletonOverlay
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
fun HomeScreen(
    viewModel: GestureControlViewModel,
    onNavigateToTest: () -> Unit,
    onNavigateToGestures: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToTraining: () -> Unit = {},
    onNavigateToAirCursor: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToGuide: () -> Unit = {}
) {
    val context = LocalContext.current
    val isControlActive by viewModel.isControlActive.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val lastGesture by viewModel.lastRecognizedGesture.collectAsState()
    val confidence by viewModel.recognitionConfidence.collectAsState()
    val lastAction by viewModel.lastExecutedAction.collectAsState()
    val lastResult by viewModel.lastExecutionResult.collectAsState()
    val handData by viewModel.currentHandData.collectAsState()
    val calibrations by viewModel.calibrations.collectAsState()

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
    val trainedCount = calibrations.count { it.value.isTrained }
    val overallStatus = viewModel.getOverallCalibrationQuality()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ==========================================
        // 1. HEADER (Section 4)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "HANDLY",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = HandlyTextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Control your phone hands-free.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HandlyVioletBright,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Your hands. Your control.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = HandlyTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // On-Device Privacy Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HandlyElevated)
                    .border(1.dp, HandlySuccess.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = HandlySuccess,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "On-Device",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HandlySuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ==========================================
        // 2. HERO SECTION (Section 4)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, HandlyViolet.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "Control without touching.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = HandlyTextPrimary,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Use simple hand gestures to navigate, tap and control your phone.",
                    fontSize = 14.sp,
                    color = HandlyTextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary & Secondary Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary Action: Start / Stop Live Control
                    Button(
                        onClick = {
                            if (isControlActive) {
                                viewModel.stopControl()
                            } else {
                                if (!hasCameraPermission) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    viewModel.startControl()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isControlActive) HandlyError else HandlyViolet
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("hero_live_control_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isControlActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isControlActive) "Stop Live Control" else "Start Live Control",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Secondary Action: Practice gestures
                    OutlinedButton(
                        onClick = onNavigateToGestures,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HandlyCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HandlyCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("hero_practice_gestures_button")
                    ) {
                        Text(
                            text = "Practice gestures",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 3. LIVE STATUS CARD (Section 5)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (isControlActive) HandlySuccess.copy(alpha = 0.6f) else HandlyViolet.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Title and Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HANDLY CONTROL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = HandlyVioletBright,
                        letterSpacing = 1.2.sp
                    )

                    // Status pill: ○ Ready or ● Live
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isControlActive) HandlySuccess.copy(alpha = 0.18f)
                                else HandlyElevated
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isControlActive) HandlySuccess else HandlyTextSecondary,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isControlActive) "● Live" else "○ Ready",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isControlActive) HandlySuccess else HandlyTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isControlActive) {
                    // INACTIVE STATE (Section 5)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(HandlyElevated)
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(HandlyViolet.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PanTool,
                                contentDescription = null,
                                tint = HandlyVioletBright,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Show your hand to get started.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HandlyTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Activate Live Control to interact with reading feeds, videos, navigation and volume hands-free.",
                            fontSize = 12.sp,
                            color = HandlyTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        Button(
                            onClick = {
                                if (!hasCameraPermission) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    viewModel.startControl()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HandlyViolet),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Live Control", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // ACTIVE STATE (Section 5): Live Telemetry with REAL RUNTIME VALUES
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(HandlyElevated)
                            .padding(16.dp)
                    ) {
                        // Hand Detected / Scanning Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (handData.isPresent) HandlySuccess else HandlyCyan,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (handData.isPresent) "Hand detected" else "No hand detected",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (handData.isPresent) HandlySuccess else HandlyTextSecondary
                                )
                            }

                            // Subsystem checklist mini badge
                            Text(
                                text = "Profile: ${activeProfile.name}",
                                fontSize = 11.sp,
                                color = HandlyTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric Columns: Gesture | Confidence | Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Column 1: Gesture
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gesture",
                                    fontSize = 11.sp,
                                    color = HandlyTextSecondary
                                )
                                Text(
                                    text = if (handData.isPresent && lastGesture != null) lastGesture!!.displayName else "None",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HandlyCyan
                                )
                            }

                            // Column 2: Confidence
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Confidence",
                                    fontSize = 11.sp,
                                    color = HandlyTextSecondary
                                )
                                val confVal = if (handData.isPresent && confidence > 0f) "${(confidence * 100).toInt()}%" else "--"
                                Text(
                                    text = confVal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HandlyTextPrimary
                                )
                            }

                            // Column 3: Action
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Action",
                                    fontSize = 11.sp,
                                    color = HandlyTextSecondary
                                )
                                Text(
                                    text = lastAction?.displayName ?: "None",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HandlyVioletBright
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Execution Result row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Execution Result:",
                                fontSize = 12.sp,
                                color = HandlyTextSecondary
                            )
                            Text(
                                text = when (lastResult) {
                                    ActionResult.SUCCESS -> "✓ Executed"
                                    ActionResult.SERVICE_DISABLED -> "Accessibility disabled"
                                    ActionResult.UNSUPPORTED -> "Unsupported"
                                    ActionResult.FAILED -> "Failed"
                                    null -> "Ready"
                                    else -> lastResult!!.name
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (lastResult) {
                                    ActionResult.SUCCESS -> HandlySuccess
                                    ActionResult.SERVICE_DISABLED, ActionResult.FAILED -> HandlyError
                                    else -> HandlyCyan
                                }
                            )
                        }
                    }
                }

                // ==========================================
                // 4. REAL HAND VISUALIZATION (Section 6)
                // ==========================================
                if (isControlActive) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "REAL-TIME VISION & SKELETON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HandlyVioletBright,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black)
                            .border(1.dp, HandlyViolet.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    ) {
                        if (hasCameraPermission) {
                            // Real Camera Feed & MediaPipe 21 Landmark Hand Skeleton Moving with Real Hand
                            CameraPreviewView(
                                handData = handData,
                                activeGesture = lastGesture,
                                onFrame = { frame -> viewModel.processFrame(frame) },
                                onCameraReady = { viewModel.setCameraReady(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = HandlyError,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Camera Permission Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HandlyViolet)
                                ) {
                                    Text("Grant Camera Access")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 5. ESSENTIAL FEATURE CARDS (TRAINING & AIR CURSOR)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gesture Training Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTraining() }
                    .testTag("home_training_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HandlySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HandlyViolet.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(HandlyViolet.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = null,
                            tint = HandlyVioletBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Gesture Training",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HandlyTextPrimary
                    )

                    Text(
                        text = "$trainedCount calibrated",
                        fontSize = 12.sp,
                        color = HandlyCyan,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Air Cursor Studio Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToAirCursor() }
                    .testTag("home_air_cursor_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HandlySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HandlyCyan.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(HandlyCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = null,
                            tint = HandlyCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Air Cursor",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HandlyTextPrimary
                    )

                    Text(
                        text = "Precision Pointer",
                        fontSize = 12.sp,
                        color = HandlySuccess,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Services & Permissions Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Camera Status
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (!hasCameraPermission) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HandlyElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = HandlyCyan, modifier = Modifier.size(18.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (hasCameraPermission) HandlySuccess else HandlyError, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Camera Sensor", fontSize = 11.sp, color = HandlyTextSecondary)
                    Text(
                        text = if (hasCameraPermission) "Ready" else "Grant Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (hasCameraPermission) HandlySuccess else HandlyCyan
                    )
                }
            }

            // Accessibility Status
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (!isAccessibilityConnected) {
                            AccessibilityController.openAccessibilitySettings(context)
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HandlyElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = HandlyVioletBright, modifier = Modifier.size(18.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isAccessibilityConnected) HandlySuccess else HandlyError, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Accessibility", fontSize = 11.sp, color = HandlyTextSecondary)
                    Text(
                        text = if (isAccessibilityConnected) "Connected" else "Enable Service",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isAccessibilityConnected) HandlySuccess else HandlyVioletBright
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
