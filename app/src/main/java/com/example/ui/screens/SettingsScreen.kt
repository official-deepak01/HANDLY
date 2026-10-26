package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CalibrationOverallStatus
import com.example.model.Cooldown
import com.example.model.HandPreference
import com.example.model.Sensitivity
import com.example.service.AccessibilityController
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
fun SettingsScreen(
    viewModel: GestureControlViewModel,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAccessibilityDisclosure: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToTraining: () -> Unit = {},
    onNavigateToAirCursor: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val activeProfile by viewModel.activeProfile.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()
    val cooldown by viewModel.cooldown.collectAsState()
    val handPreference by viewModel.handPreference.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()
    val isVisualFeedbackEnabled by viewModel.isVisualFeedbackEnabled.collectAsState()
    val isAutoProfileSwitchEnabled by viewModel.isAutoProfileSwitchEnabled.collectAsState()
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

    val trainedCount = calibrations.count { it.value.isTrained }
    val overallStatus = viewModel.getOverallCalibrationQuality()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // HEADER
        // ==========================================
        Column {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = HandlyTextPrimary
            )
            Text(
                text = "System preferences, sensitivity & engine status",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = HandlyVioletBright,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // ==========================================
        // 1. PROFILE CARD (Section 10)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, HandlyViolet.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(HandlyViolet.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = HandlyVioletBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = HandlyTextPrimary
                            )
                            Text(
                                text = "Current: ${activeProfile.name}",
                                fontSize = 12.sp,
                                color = HandlyCyan
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(HandlyElevated)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${activeProfile.mappings.size} mappings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HandlyTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto Profile Switching Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-switch profiles by foreground app", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = HandlyTextPrimary)
                        Text("Switches to YouTube, Reader, or Gallery when opened", fontSize = 11.sp, color = HandlyTextSecondary)
                    }
                    Switch(
                        checked = isAutoProfileSwitchEnabled,
                        onCheckedChange = { viewModel.setAutoProfileSwitch(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HandlySuccess,
                            checkedTrackColor = HandlySuccess.copy(alpha = 0.35f)
                        )
                    )
                }
            }
        }

        // ==========================================
        // 2. DETECTION SENSITIVITY (Section 10)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, HandlyCyan.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(HandlyCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = HandlyCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Detection Sensitivity",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = HandlyTextPrimary
                        )
                        Text(
                            text = "Tune gesture trigger threshold",
                            fontSize = 12.sp,
                            color = HandlyTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sensitivity selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Sensitivity.values().forEach { s ->
                        val isSelected = sensitivity == s
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSensitivity(s) },
                            label = { Text(s.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HandlyCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = HandlyElevated,
                                labelColor = HandlyTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) HandlyCyan else Color.Transparent,
                                borderWidth = 1.dp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cooldown Row
                Text(
                    text = "Gesture Cooldown Period",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HandlyTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Cooldown.values().forEach { c ->
                        val isSelected = cooldown == c
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCooldown(c) },
                            label = { Text(c.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HandlyViolet,
                                selectedLabelColor = Color.White,
                                containerColor = HandlyElevated,
                                labelColor = HandlyTextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hand Preference
                Text(
                    text = "Hand Preference",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HandlyTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HandPreference.values().forEach { pref ->
                        val isSelected = handPreference == pref
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setHandPreference(pref) },
                            label = { Text(pref.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HandlyViolet,
                                selectedLabelColor = Color.White,
                                containerColor = HandlyElevated,
                                labelColor = HandlyTextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. CALIBRATION (Section 10)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToTraining() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, HandlyViolet.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(HandlyViolet.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PanTool,
                            contentDescription = null,
                            tint = HandlyVioletBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Calibration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = HandlyTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (overallStatus) {
                                            CalibrationOverallStatus.EXCELLENT -> HandlySuccess.copy(alpha = 0.2f)
                                            CalibrationOverallStatus.GOOD -> HandlyCyan.copy(alpha = 0.2f)
                                            CalibrationOverallStatus.FAIR -> HandlyWarning.copy(alpha = 0.2f)
                                            CalibrationOverallStatus.UNCALIBRATED -> Color.Gray.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = overallStatus.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (overallStatus) {
                                        CalibrationOverallStatus.EXCELLENT -> HandlySuccess
                                        CalibrationOverallStatus.GOOD -> HandlyCyan
                                        CalibrationOverallStatus.FAIR -> HandlyWarning
                                        CalibrationOverallStatus.UNCALIBRATED -> Color.Gray
                                    }
                                )
                            }
                        }
                        Text(
                            text = "$trainedCount of 11 gestures calibrated • Tap to calibrate",
                            fontSize = 12.sp,
                            color = HandlyTextSecondary
                        )
                    }
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = HandlyTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ==========================================
        // 4. ACCESSIBILITY STATUS (Section 10)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isAccessibilityConnected) HandlySuccess.copy(alpha = 0.3f) else HandlyError.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isAccessibilityConnected) HandlySuccess.copy(alpha = 0.15f)
                                    else HandlyError.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = if (isAccessibilityConnected) HandlySuccess else HandlyError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Accessibility Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = HandlyTextPrimary
                            )
                            Text(
                                text = if (isAccessibilityConnected) "Connected & ready" else "Disabled — Actions cannot execute",
                                fontSize = 12.sp,
                                color = if (isAccessibilityConnected) HandlySuccess else HandlyError
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isAccessibilityConnected) HandlySuccess else HandlyError, CircleShape)
                    )
                }

                if (!isAccessibilityConnected) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { AccessibilityController.openAccessibilitySettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = HandlyViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Enable Accessibility Service", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // ==========================================
        // 5. CAMERA STATUS (Section 10)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (hasCameraPermission) HandlySuccess.copy(alpha = 0.3f) else HandlyError.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (hasCameraPermission) HandlySuccess.copy(alpha = 0.15f)
                                    else HandlyError.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (hasCameraPermission) HandlySuccess else HandlyError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Camera Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = HandlyTextPrimary
                            )
                            Text(
                                text = if (hasCameraPermission) "Camera ready • 30 FPS" else "Permission missing",
                                fontSize = 12.sp,
                                color = if (hasCameraPermission) HandlySuccess else HandlyError
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (hasCameraPermission) HandlySuccess else HandlyError, CircleShape)
                    )
                }

                if (!hasCameraPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = HandlyViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // ==========================================
        // 6. ABOUT HANDLY (Section 10)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, HandlyViolet.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(HandlyViolet.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = HandlyVioletBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "About HANDLY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = HandlyTextPrimary
                        )
                        Text(
                            text = "Version 1.0.0 • 100% On-Device",
                            fontSize = 12.sp,
                            color = HandlyTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "HANDLY is a touchless hand gesture controller for Android. Built with MediaPipe Tasks-Vision. All camera analysis runs locally on your device without sending video frames or telemetry to external servers.",
                    fontSize = 12.sp,
                    color = HandlyTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HandlyElevated)
                        .clickable { onNavigateToPrivacy() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = HandlySuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Information", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = HandlyTextPrimary)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = HandlyTextSecondary, modifier = Modifier.size(14.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HandlyElevated)
                        .clickable { onNavigateToAccessibilityDisclosure() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = HandlyVioletBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accessibility Service Disclosure", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = HandlyTextPrimary)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = HandlyTextSecondary, modifier = Modifier.size(14.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HandlyElevated)
                        .clickable { onNavigateToHelp() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = HandlyCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Help Center & Tutorials", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = HandlyTextPrimary)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = HandlyTextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
