package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.model.CalibrationOverallStatus
import com.example.model.GestureCalibrationData
import com.example.model.GestureType
import com.example.model.HandDistance
import com.example.model.HandPreference
import com.example.ui.components.CameraPreviewView
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.AlertRose
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TechCyanLight
import com.example.ui.theme.TechIndigo
import com.example.viewmodel.GestureControlViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureTrainingScreen(
    viewModel: GestureControlViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val calibrations by viewModel.calibrations.collectAsState()
    val handPreference by viewModel.handPreference.collectAsState()
    val trainingProgress by viewModel.trainingProgress.collectAsState()
    val handData by viewModel.currentHandData.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        viewModel.setCameraReady(granted)
    }

    val supportedGestures = listOf(
        GestureType.OPEN_PALM,
        GestureType.FIST,
        GestureType.THUMBS_UP,
        GestureType.THUMBS_DOWN,
        GestureType.INDEX_POINT,
        GestureType.TWO_FINGERS,
        GestureType.PINCH,
        GestureType.SWIPE_UP,
        GestureType.SWIPE_DOWN,
        GestureType.SWIPE_LEFT,
        GestureType.SWIPE_RIGHT
    )

    var activeTrainingGesture by remember { mutableStateOf<GestureType?>(null) }
    var isTrainAllMode by remember { mutableStateOf(false) }
    var trainAllIndex by remember { mutableIntStateOf(0) }

    val trainedCount = supportedGestures.count { calibrations[it]?.isTrained == true }
    val overallStatus = when {
        trainedCount == 0 -> CalibrationOverallStatus.UNCALIBRATED
        trainedCount in 1..4 -> CalibrationOverallStatus.FAIR
        trainedCount in 5..8 -> CalibrationOverallStatus.GOOD
        else -> CalibrationOverallStatus.EXCELLENT
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Training", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Calibration Quality Dashboard Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GESTURE TRAINING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TechCyan,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Personal Calibration",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Overall status badge
                            val (badgeBg, badgeText) = when (overallStatus) {
                                CalibrationOverallStatus.UNCALIBRATED -> Color.Gray.copy(alpha = 0.2f) to Color.Gray
                                CalibrationOverallStatus.FAIR -> AmberWarning.copy(alpha = 0.2f) to AmberWarning
                                CalibrationOverallStatus.GOOD -> TechCyan.copy(alpha = 0.2f) to TechCyan
                                CalibrationOverallStatus.EXCELLENT -> ActiveEmerald.copy(alpha = 0.2f) to ActiveEmerald
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = overallStatus.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { trainedCount / supportedGestures.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = TechCyan,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$trainedCount of ${supportedGestures.size} gestures calibrated for your hand style and distance.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Hand Preference Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hand Profile:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HandPreference.values().forEach { pref ->
                                    val isSelected = handPreference == pref
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) TechCyan else MaterialTheme.colorScheme.surface)
                                            .clickable { viewModel.setHandPreference(pref) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = pref.displayName.replace(" Hand", ""),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dashboard Actions: Train All & Reset All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!hasCameraPermission) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        isTrainAllMode = true
                                        trainAllIndex = 0
                                        activeTrainingGesture = supportedGestures[0]
                                        viewModel.startTrainingGesture(supportedGestures[0])
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Train All", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetAllCalibrations() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset All")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "CALIBRATION STATUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            // 2. Gesture List Items
            items(supportedGestures) { gesture ->
                val cal = calibrations[gesture]
                val isTrained = cal?.isTrained == true

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Status icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isTrained) ActiveEmerald.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isTrained) "✓" else "○",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isTrained) ActiveEmerald else Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = gesture.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isTrained) "${cal?.qualityPercentage}% Quality • ${cal?.sampleCount} samples"
                                    else "Not calibrated",
                                    fontSize = 12.sp,
                                    color = if (isTrained) ActiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Buttons: Train / Retrain & Reset
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isTrained) {
                                IconButton(
                                    onClick = { viewModel.resetCalibration(gesture) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Reset",
                                        tint = AlertRose.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (!hasCameraPermission) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        isTrainAllMode = false
                                        activeTrainingGesture = gesture
                                        viewModel.startTrainingGesture(gesture)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTrained) MaterialTheme.colorScheme.surface else TechCyan,
                                    contentColor = if (isTrained) MaterialTheme.colorScheme.onSurface else Color.Black
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = if (isTrained) "Retrain" else "Train",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // 3. Interactive Training Modal / Flow
    if (activeTrainingGesture != null && hasCameraPermission) {
        val gestureToTrain = activeTrainingGesture!!
        var countdown by remember { mutableIntStateOf(3) }
        var isCalibratedInSession by remember { mutableStateOf<GestureCalibrationData?>(null) }

        // 3-second countdown before sampling begins
        LaunchedEffect(gestureToTrain) {
            countdown = 3
            isCalibratedInSession = null
            while (countdown > 0) {
                delay(1000L)
                countdown--
            }
        }

        // Auto-finish when enough valid samples are collected
        LaunchedEffect(trainingProgress?.isComplete) {
            if (trainingProgress?.isComplete == true && isCalibratedInSession == null) {
                val savedData = viewModel.finishTraining()
                isCalibratedInSession = savedData
            }
        }

        Dialog(
            onDismissRequest = {
                viewModel.cancelTraining()
                activeTrainingGesture = null
                isTrainAllMode = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Live Camera View with Hand Skeleton Overlay
                    CameraPreviewView(
                        handData = handData,
                        activeGesture = gestureToTrain,
                        onFrame = { proxy -> viewModel.processFrame(proxy) },
                        onCameraReady = { viewModel.setCameraReady(it) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Header Bar with Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${gestureToTrain.displayName} Training",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (isTrainAllMode) {
                                Text(
                                    text = "Step ${trainAllIndex + 1} of ${supportedGestures.size}",
                                    fontSize = 12.sp,
                                    color = TechCyanLight
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.cancelTraining()
                                activeTrainingGesture = null
                                isTrainAllMode = false
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Countdown Overlay
                    if (countdown > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Get Ready",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "$countdown",
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TechCyan
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = getInstructionForGesture(gestureToTrain),
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Active Sampling or Finished State Card at Bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        if (isCalibratedInSession != null) {
                            // Success state
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ActiveEmerald, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${gestureToTrain.displayName} calibrated successfully.",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ActiveEmerald
                                    )
                                    val q = isCalibratedInSession?.qualityPercentage ?: 92
                                    val samples = isCalibratedInSession?.sampleCount ?: 35
                                    Text(
                                        text = "Quality: $q% • Samples: $samples",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isCalibratedInSession = null
                                        countdown = 3
                                        viewModel.startTrainingGesture(gestureToTrain)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Retrain", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        if (isTrainAllMode && trainAllIndex + 1 < supportedGestures.size) {
                                            trainAllIndex++
                                            val nextGesture = supportedGestures[trainAllIndex]
                                            activeTrainingGesture = nextGesture
                                            isCalibratedInSession = null
                                            countdown = 3
                                            viewModel.startTrainingGesture(nextGesture)
                                        } else {
                                            activeTrainingGesture = null
                                            isTrainAllMode = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                                ) {
                                    val btnText = if (isTrainAllMode && trainAllIndex + 1 < supportedGestures.size) "Next Gesture" else "Finish"
                                    Text(btnText, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        } else {
                            // Active Sampling state
                            val progress = trainingProgress
                            val collected = progress?.collectedCount ?: 0
                            val target = progress?.targetCount ?: 35
                            val percent = if (target > 0) ((collected.toFloat() / target) * 100).toInt() else 0

                            Text(
                                text = getInstructionForGesture(gestureToTrain),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { (collected / target.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = TechCyan,
                                trackColor = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = progress?.feedbackMessage ?: "Collecting samples...",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "$percent% ($collected/$target)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TechCyanLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getInstructionForGesture(gesture: GestureType): String {
    return when (gesture) {
        GestureType.OPEN_PALM -> "Hold your palm open and flat facing the camera."
        GestureType.FIST -> "Curl all your fingers into a closed fist facing the camera."
        GestureType.THUMBS_UP -> "Close your fingers and extend your thumb upwards."
        GestureType.THUMBS_DOWN -> "Close your fingers and point your thumb downwards."
        GestureType.INDEX_POINT -> "Extend your index finger upwards while curling other fingers."
        GestureType.TWO_FINGERS -> "Extend index and middle fingers in a V sign."
        GestureType.PINCH -> "Bring your thumb and index fingertips close together."
        GestureType.OK_SIGN -> "Touch your thumb and index fingertips in an O shape with other fingers extended."
        GestureType.SWIPE_UP -> "Move your open hand smoothly upwards across the camera."
        GestureType.SWIPE_DOWN -> "Move your open hand smoothly downwards across the camera."
        GestureType.SWIPE_LEFT -> "Move your hand smoothly to the left."
        GestureType.SWIPE_RIGHT -> "Move your hand smoothly to the right."
    }
}
