package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.BatteryMode
import com.example.service.AccessibilityController
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.TechCyan
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.GestureControlViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: GestureControlViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val debugStats by viewModel.debugStats.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val batteryMode by viewModel.batteryMode.collectAsState()
    val isControlActive by viewModel.isControlActive.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()

    var testActionResult by remember { mutableStateOf<Pair<ActionType, ActionResult>?>(null) }
    var testErrorMessage by remember { mutableStateOf<String?>(null) }

    val canDrawOverlays = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics & Health", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("diagnostics_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkCanvas,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DarkCanvas
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Subsystem Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isControlActive) ActiveEmerald.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (isControlActive) "SERVICE ACTIVE" else "SERVICE IDLE",
                                color = if (isControlActive) ActiveEmerald else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    StatusRow(
                        label = "Accessibility Service",
                        status = if (isAccessibilityConnected) "Connected & Bound" else "Disconnected",
                        isOk = isAccessibilityConnected
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusRow(
                        label = "Overlay Permission",
                        status = if (canDrawOverlays) "Granted" else "Missing Permission",
                        isOk = canDrawOverlays
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusRow(
                        label = "Foreground Window",
                        status = AccessibilityController.foregroundPackage.value ?: "com.example (Gesture Control)",
                        isOk = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusRow(
                        label = "Active App Profile",
                        status = activeProfile.name,
                        isOk = true
                    )
                }
            }

            // Real-time Vision Engine Telemetry
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = TechCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Vision & Camera Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Camera FPS",
                            value = "${debugStats.cameraFps}",
                            unit = "fps",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Latency",
                            value = "${debugStats.frameProcessingTimeMs}",
                            unit = "ms",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Confidence",
                            value = "${(debugStats.handConfidence * 100).roundToInt()}",
                            unit = "%",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(
                            title = "Landmarks",
                            value = "${debugStats.landmarkCount}",
                            unit = "/21",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Dropped Frames",
                            value = "${debugStats.droppedFramesCount}",
                            unit = "frames",
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Hand Tracking",
                            value = if (debugStats.landmarkCount > 0) "Locked" else "Searching",
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Power & Thermal Modes
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Power & Thermal Optimization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BatteryMode.values().forEach { mode ->
                            FilterChip(
                                selected = batteryMode == mode,
                                onClick = { viewModel.setBatteryMode(mode) },
                                label = { Text(mode.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarningAmber.copy(alpha = 0.25f),
                                    selectedLabelColor = WarningAmber,
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.weight(1f).testTag("battery_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        "${batteryMode.displayName}: ${batteryMode.targetFps} FPS target (${batteryMode.frameIntervalMs}ms per frame) to prevent thermal rise during extended touchless sessions.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Live System Action Verifier
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Live System Action Verifier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Test direct dispatch through AccessibilityService to confirm execution on your Android device.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    val testActions = listOf(
                        ActionType.BACK,
                        ActionType.HOME,
                        ActionType.RECENTS,
                        ActionType.NOTIFICATIONS,
                        ActionType.QUICK_SETTINGS,
                        ActionType.SCROLL_FORWARD,
                        ActionType.SCROLL_BACKWARD
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        testActions.chunked(2).forEach { rowActions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowActions.forEach { action ->
                                    OutlinedButton(
                                        onClick = {
                                            val engine = com.example.action.ActionEngine(context)
                                            val outcome = engine.executeAction(action)
                                            testActionResult = Pair(action, outcome.result)
                                            testErrorMessage = outcome.errorMessage
                                        },
                                        modifier = Modifier.weight(1f).testTag("test_action_${action.name.lowercase()}")
                                    ) {
                                        Text(action.displayName, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                                if (rowActions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (testActionResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (testActionResult?.second == ActionResult.SUCCESS) ActiveEmerald.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (testActionResult?.second == ActionResult.SUCCESS) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (testActionResult?.second == ActionResult.SUCCESS) ActiveEmerald else AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${testActionResult?.first?.displayName}: ${testActionResult?.second?.name}${if (testErrorMessage != null) " ($testErrorMessage)" else ""}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, status: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOk) ActiveEmerald else AlertRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                status,
                color = if (isOk) Color.White else AlertRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = TechCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(unit, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
    }
}
