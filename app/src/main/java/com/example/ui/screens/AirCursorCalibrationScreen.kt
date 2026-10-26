package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CursorCalibrationData
import com.example.model.CursorPoint
import com.example.ui.components.CameraPreviewView
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.TechCyan
import com.example.viewmodel.GestureControlViewModel
import kotlinx.coroutines.delay

enum class CalibrationStep(val title: String, val instruction: String, val targetNormX: Float, val targetNormY: Float) {
    CENTER("Center Reach", "Aim your index finger naturally at the center dot", 0.5f, 0.5f),
    TOP_LEFT("Top-Left Reach", "Move your finger to your comfortable top-left corner", 0.2f, 0.2f),
    TOP_RIGHT("Top-Right Reach", "Move your finger to your comfortable top-right corner", 0.8f, 0.2f),
    BOTTOM_LEFT("Bottom-Left Reach", "Move your finger to your comfortable bottom-left corner", 0.2f, 0.8f),
    BOTTOM_RIGHT("Bottom-Right Reach", "Move your finger to your comfortable bottom-right corner", 0.8f, 0.8f),
    DONE("Calibration Complete", "Your personal arm and hand reach profile has been saved!", 0.5f, 0.5f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirCursorCalibrationScreen(
    viewModel: GestureControlViewModel,
    onNavigateBack: () -> Unit
) {
    val handData by viewModel.currentHandData.collectAsState()
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var holdProgress by remember { mutableFloatStateOf(0f) }

    // Recorded landmark points
    var centerPoint by remember { mutableStateOf(Pair(0.5f, 0.5f)) }
    var minX by remember { mutableFloatStateOf(0.15f) }
    var maxX by remember { mutableFloatStateOf(0.85f) }
    var minY by remember { mutableFloatStateOf(0.15f) }
    var maxY by remember { mutableFloatStateOf(0.85f) }

    val steps = CalibrationStep.values()
    val currentStep = steps[currentStepIndex.coerceAtMost(steps.size - 1)]

    DisposableEffect(Unit) {
        viewModel.setTestMode(true)
        onDispose {
            viewModel.setTestMode(false)
        }
    }

    // Progress timer loop when hand is pointing
    LaunchedEffect(currentStepIndex, handData.isPresent) {
        if (currentStepIndex >= steps.size - 1) return@LaunchedEffect
        holdProgress = 0f

        while (true) {
            delay(50)
            if (handData.isPresent && handData.landmarks.size > 8) {
                holdProgress += 0.05f
                if (holdProgress >= 1.0f) {
                    val indexLandmark = handData.landmarks[8]
                    val x = 1f - indexLandmark.x // mirrored
                    val y = indexLandmark.y

                    when (currentStep) {
                        CalibrationStep.CENTER -> centerPoint = Pair(x, y)
                        CalibrationStep.TOP_LEFT -> {
                            minX = x
                            minY = y
                        }
                        CalibrationStep.TOP_RIGHT -> {
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                        }
                        CalibrationStep.BOTTOM_LEFT -> {
                            if (x < minX) minX = x
                            if (y > maxY) maxY = y
                        }
                        CalibrationStep.BOTTOM_RIGHT -> {
                            if (x > maxX) maxX = x
                            if (y > maxY) maxY = y
                        }
                        CalibrationStep.DONE -> {}
                    }

                    currentStepIndex++
                    holdProgress = 0f
                    break
                }
            } else {
                if (holdProgress > 0f) holdProgress -= 0.04f
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reach Calibration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("calibration_back_button")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Camera feed and hand skeleton in background
            CameraPreviewView(
                handData = handData,
                activeGesture = null,
                onFrame = { imageProxy ->
                    viewModel.processFrame(imageProxy)
                },
                onCameraReady = { viewModel.setCameraReady(it) },
                modifier = Modifier.fillMaxSize()
            )

            // Target Ring Canvas
            if (currentStep != CalibrationStep.DONE) {
                CalibrationTargetCanvas(
                    targetNormX = currentStep.targetNormX,
                    targetNormY = currentStep.targetNormY,
                    holdProgress = holdProgress,
                    handData = handData,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Progress Indicator
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currentStep.title,
                                fontWeight = FontWeight.Bold,
                                color = TechCyan,
                                fontSize = 16.sp
                            )
                            Text(
                                "Step ${currentStepIndex + 1} of ${steps.size}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ((currentStepIndex + holdProgress) / steps.size.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = TechCyan,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            currentStep.instruction,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Bottom Actions Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (currentStep == CalibrationStep.DONE) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ActiveEmerald,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Reach Calibrated Successfully!",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Pointer bounding envelope: X=[${(minX * 100).toInt()}% .. ${(maxX * 100).toInt()}%], Y=[${(minY * 100).toInt()}% .. ${(maxY * 100).toInt()}%]",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val calib = CursorCalibrationData(
                                    center = CursorPoint(centerPoint.first, centerPoint.second),
                                    topLeft = CursorPoint(minX.coerceIn(0.05f, 0.45f), minY.coerceIn(0.05f, 0.45f)),
                                    topRight = CursorPoint(maxX.coerceIn(0.55f, 0.95f), minY.coerceIn(0.05f, 0.45f)),
                                    bottomLeft = CursorPoint(minX.coerceIn(0.05f, 0.45f), maxY.coerceIn(0.55f, 0.95f)),
                                    bottomRight = CursorPoint(maxX.coerceIn(0.55f, 0.95f), maxY.coerceIn(0.55f, 0.95f)),
                                    isCalibrated = true
                                )
                                viewModel.saveCursorCalibration(calib)
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveEmerald),
                            modifier = Modifier.fillMaxWidth().testTag("save_calibration_button")
                        ) {
                            Text("Save & Finish", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStepIndex = 0
                                    holdProgress = 0f
                                },
                                modifier = Modifier.weight(1f).testTag("restart_calibration_button")
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restart")
                            }

                            Button(
                                onClick = {
                                    currentStepIndex = (currentStepIndex + 1).coerceAtMost(steps.size - 1)
                                    holdProgress = 0f
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                                modifier = Modifier.weight(1f).testTag("next_step_button")
                            ) {
                                Text("Skip Step", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalibrationTargetCanvas(
    targetNormX: Float,
    targetNormY: Float,
    holdProgress: Float,
    handData: com.example.vision.HandData,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val tx = targetNormX * size.width
        val ty = targetNormY * size.height

        // Outer Target Ring
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
            radius = 44.dp.toPx(),
            center = Offset(tx, ty)
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 44.dp.toPx(),
            center = Offset(tx, ty),
            style = Stroke(width = 2.dp.toPx())
        )

        // Hold progress arc
        if (holdProgress > 0f) {
            drawArc(
                color = Color(0xFF00E676),
                startAngle = -90f,
                sweepAngle = holdProgress * 360f,
                useCenter = false,
                topLeft = Offset(tx - 44.dp.toPx(), ty - 44.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(88.dp.toPx(), 88.dp.toPx()),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Center dot
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(tx, ty)
        )

        // If hand is detected, show current index finger position
        if (handData.isPresent && handData.landmarks.size > 8) {
            val idx = handData.landmarks[8]
            val ix = (1f - idx.x) * size.width
            val iy = idx.y * size.height
            drawCircle(
                color = Color(0xFFFFD600),
                radius = 10.dp.toPx(),
                center = Offset(ix, iy)
            )
        }
    }
}
