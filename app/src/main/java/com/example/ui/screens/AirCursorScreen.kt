package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ControlMode
import com.example.model.CursorInteractionType
import com.example.model.PinchBehavior
import com.example.ui.components.CameraPreviewView
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.TechCyan
import com.example.viewmodel.GestureControlViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirCursorScreen(
    viewModel: GestureControlViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCalibration: () -> Unit
) {
    val context = LocalContext.current
    val controlMode by viewModel.controlMode.collectAsState()
    val cursorState by viewModel.cursorState.collectAsState()
    val handData by viewModel.currentHandData.collectAsState()
    val cursorSensitivity by viewModel.cursorSensitivity.collectAsState()
    val cursorSmoothing by viewModel.cursorSmoothing.collectAsState()
    val cursorAcceleration by viewModel.cursorAcceleration.collectAsState()
    val cursorDeadZone by viewModel.cursorDeadZone.collectAsState()
    val cursorClickThreshold by viewModel.cursorClickThreshold.collectAsState()
    val cursorClickCooldown by viewModel.cursorClickCooldown.collectAsState()
    val isDwellClickEnabled by viewModel.isDwellClickEnabled.collectAsState()
    val dwellDurationMs by viewModel.dwellDurationMs.collectAsState()
    val pinchBehavior by viewModel.pinchBehavior.collectAsState()
    val isFloatingOverlayEnabled by viewModel.isFloatingOverlayEnabled.collectAsState()
    val cursorCalibration by viewModel.cursorCalibration.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()

    // Test playground counters
    var testClickCount by remember { mutableIntStateOf(0) }
    var testTargetActive by remember { mutableStateOf(false) }

    // Enable test mode while on this screen
    DisposableEffect(Unit) {
        viewModel.setTestMode(true)
        onDispose {
            viewModel.setTestMode(false)
        }
    }

    val canDrawOverlays = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Air Cursor Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("air_cursor_back_button")
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
            // Mode Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Active Operating Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ControlMode.values().forEach { mode ->
                            FilterChip(
                                selected = controlMode == mode,
                                onClick = { viewModel.setControlMode(mode) },
                                label = { Text(mode.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TechCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = TechCyan,
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.weight(1f).testTag("chip_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        controlMode.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Live Interactive Cursor Test Stage
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .testTag("air_cursor_preview_stage")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Camera feed and hand skeleton
                    CameraPreviewView(
                        handData = handData,
                        activeGesture = null,
                        onFrame = { imageProxy ->
                            viewModel.processFrame(imageProxy)
                        },
                        onCameraReady = { viewModel.setCameraReady(it) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Test target buttons inside the canvas
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (testTargetActive) ActiveEmerald.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, if (testTargetActive) ActiveEmerald else Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable {
                                testClickCount++
                                testTargetActive = !testTargetActive
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Target: $testClickCount clicks",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Interactive Air Cursor Overlay on the preview
                    AirCursorVisualOverlay(
                        cursorState = cursorState,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bottom info strip
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (cursorState.isPointing) "Pointing Active" else "Hold index finger up",
                                color = if (cursorState.isPointing) TechCyan else Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (cursorState.isPinching) "PINCH / CLICK" else "Pinch thumb+index to click",
                                color = if (cursorState.isPinching) ActiveEmerald else Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Floating Overlay Banner (if not permitted)
            if (!canDrawOverlays && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Display Over Other Apps",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                "Required to show the Air Cursor pointer across Android while using other applications.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            modifier = Modifier.testTag("grant_overlay_permission_button")
                        ) {
                            Text("Grant", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Pointer Reach Calibration Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Adjust, contentDescription = null, tint = TechCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Reach Calibration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (cursorCalibration.isCalibrated) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ActiveEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Calibrated", color = ActiveEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Calibrate your personal physical arm and hand reach to comfortably access all corners of the display.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToCalibration,
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                            modifier = Modifier.weight(1f).testTag("start_calibration_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Calibrate Reach", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        if (cursorCalibration.isCalibrated) {
                            OutlinedButton(
                                onClick = { viewModel.resetCursorCalibration() },
                                modifier = Modifier.testTag("reset_calibration_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // Air Cursor Sensitivity & Smoothing Controls
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Pointer Dynamics & Tuning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pointer Sensitivity", color = Color.White, fontSize = 13.sp)
                            Text("${(cursorSensitivity * 100).roundToInt()}%", color = TechCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = cursorSensitivity,
                            onValueChange = { viewModel.setCursorSensitivity(it) },
                            valueRange = 0.5f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = TechCyan, activeTrackColor = TechCyan),
                            modifier = Modifier.testTag("slider_cursor_sensitivity")
                        )
                    }

                    // Smoothing Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Motion Smoothing (Anti-Jitter)", color = Color.White, fontSize = 13.sp)
                            Text("${(cursorSmoothing * 100).roundToInt()}%", color = TechCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = cursorSmoothing,
                            onValueChange = { viewModel.setCursorSmoothing(it) },
                            valueRange = 0.2f..0.9f,
                            colors = SliderDefaults.colors(thumbColor = TechCyan, activeTrackColor = TechCyan),
                            modifier = Modifier.testTag("slider_cursor_smoothing")
                        )
                    }

                    // Pinch Click Sensitivity
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pinch Click Threshold", color = Color.White, fontSize = 13.sp)
                            Text(if (cursorClickThreshold < 0.06f) "Tight" else if (cursorClickThreshold > 0.09f) "Easy" else "Normal", color = TechCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = cursorClickThreshold,
                            onValueChange = { viewModel.setCursorClickThreshold(it) },
                            valueRange = 0.04f..0.12f,
                            colors = SliderDefaults.colors(thumbColor = TechCyan, activeTrackColor = TechCyan),
                            modifier = Modifier.testTag("slider_click_threshold")
                        )
                    }

                    // Acceleration Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pointer Acceleration", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Allows fast motions to travel further across the screen", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = cursorAcceleration,
                            onCheckedChange = { viewModel.setCursorAcceleration(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TechCyan, checkedTrackColor = TechCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("switch_cursor_acceleration")
                        )
                    }

                    // Dwell Click Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dwell to Click", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Automatically clicks when pointer hovers still on a spot", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = isDwellClickEnabled,
                            onCheckedChange = { viewModel.setDwellClickEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TechCyan, checkedTrackColor = TechCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("switch_dwell_click")
                        )
                    }

                    // Floating Overlay Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("System Floating Pointer", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Draws the Air Cursor on top of all other apps during Live Control", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = isFloatingOverlayEnabled,
                            onCheckedChange = { viewModel.setFloatingOverlayEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TechCyan, checkedTrackColor = TechCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("switch_floating_overlay")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AirCursorVisualOverlay(
    cursorState: com.example.model.CursorState,
    modifier: Modifier = Modifier
) {
    if (!cursorState.isPointing && cursorState.interaction == CursorInteractionType.NONE) return

    val animatedPinchScale by animateFloatAsState(
        targetValue = if (cursorState.isPinching) 0.8f else 1.0f,
        animationSpec = tween(120),
        label = "pinch_scale"
    )

    Canvas(modifier = modifier) {
        val cx = cursorState.normalizedX * size.width
        val cy = cursorState.normalizedY * size.height

        val ringRadius = 24.dp.toPx() * animatedPinchScale
        val dotRadius = 6.dp.toPx()

        val ringColor = if (cursorState.isPinching) {
            Color(0xFF00E676)
        } else {
            Color(0xFF00E5FF)
        }

        // Dwell click progress arc
        if (cursorState.dwellProgress > 0f) {
            drawArc(
                color = Color(0xFFFFD600),
                startAngle = -90f,
                sweepAngle = cursorState.dwellProgress * 360f,
                useCenter = false,
                topLeft = Offset(cx - ringRadius - 6f, cy - ringRadius - 6f),
                size = androidx.compose.ui.geometry.Size((ringRadius + 6f) * 2, (ringRadius + 6f) * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Outer Target Ring
        drawCircle(
            color = ringColor.copy(alpha = 0.35f),
            radius = ringRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = ringColor,
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )

        // Center Pointer Core
        drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = Offset(cx, cy)
        )
    }
}
