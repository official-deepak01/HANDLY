package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.GestureType
import com.example.service.AccessibilityController
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.HandGestureIllustration
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
import com.example.vision.HandData

@Composable
fun OnboardingScreen(
    viewModel: GestureControlViewModel,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val context = LocalContext.current

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
        if (isGranted) {
            currentStep = 5
        }
    }

    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val currentHandData by viewModel.currentHandData.collectAsState()
    val lastRecognizedGesture by viewModel.lastRecognizedGesture.collectAsState()
    val confidence by viewModel.recognitionConfidence.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Step indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (step in 1..6) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (step == currentStep) 24.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (step == currentStep) HandlyViolet
                                else if (step < currentStep) HandlySuccess
                                else HandlyElevated
                            )
                    )
                }
            }

            // Main Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "OnboardingContent"
            ) { step ->
                when (step) {
                    1 -> Page1ControlWithoutTouching()
                    2 -> Page2NaturalGestures()
                    3 -> Page3PrivateAndSafe()
                    4 -> Step4CameraPermission(
                        hasPermission = hasCameraPermission,
                        onRequestPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                    5 -> Step5Accessibility(
                        isConnected = isAccessibilityConnected,
                        onOpenSettings = {
                            AccessibilityController.openAccessibilitySettings(context)
                        }
                    )
                    6 -> Step6Ready(
                        isCameraReady = hasCameraPermission,
                        isAccessibilityReady = isAccessibilityConnected
                    )
                }
            }

            // Bottom Navigation Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1 && currentStep < 6) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HandlyTextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HandlyElevated)
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 6) {
                            if (currentStep == 4 && !hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                currentStep++
                            }
                        } else {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == 6) HandlySuccess else HandlyViolet
                    ),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentStep == 6) "Get Started with HANDLY" else "Continue",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep == 6) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section 11 - Page 1
 * Control without touching.
 * Your phone, controlled with simple hand gestures.
 */
@Composable
private fun Page1ControlWithoutTouching() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(HandlyViolet.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, HandlyViolet.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PanTool,
                contentDescription = null,
                tint = HandlyVioletBright,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "HANDLY",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HandlyCyan,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Control without touching.",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = HandlyTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your phone, controlled with simple hand gestures.",
            fontSize = 16.sp,
            color = HandlyVioletBright,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Navigate reading feeds, control video playback, switch apps and adjust volume hands-free using your device camera.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = HandlyTextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp),
            lineHeight = 20.sp
        )
    }
}

/**
 * Section 11 - Page 2
 * Natural gestures.
 * Swipe to scroll, pinch to tap, open palm to pause.
 */
@Composable
private fun Page2NaturalGestures() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Natural gestures.",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = HandlyTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Swipe to scroll, pinch to tap, open palm to pause.",
            fontSize = 15.sp,
            color = HandlyVioletBright,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        val gestureExamples = listOf(
            Triple(GestureType.SWIPE_UP, "Swipe to scroll", "Glide hand up or down to scroll feeds"),
            Triple(GestureType.PINCH, "Pinch to tap", "Touch thumb and index to select and click"),
            Triple(GestureType.OPEN_PALM, "Open palm to pause", "Show an open hand to toggle playback")
        )

        gestureExamples.forEach { (gesture, title, sub) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HandlySurface)
                    .border(1.dp, HandlyViolet.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HandGestureIllustration(
                    gesture = gesture,
                    size = 46.dp,
                    tint = HandlyCyan
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HandlyTextPrimary)
                    Text(text = sub, color = HandlyTextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Section 11 - Page 3
 * Private and safe.
 * All processing happens directly on your device.
 */
@Composable
private fun Page3PrivateAndSafe() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(HandlySuccess.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, HandlySuccess.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = HandlySuccess,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Private and safe.",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = HandlyTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "All processing happens directly on your device.",
            fontSize = 15.sp,
            color = HandlySuccess,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, HandlySuccess.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrivacyItem(title = "Zero Cloud Uploads", subtitle = "Camera frames never leave your phone's memory.")
                PrivacyItem(title = "No Video Saved", subtitle = "Frames are analyzed instantaneously in RAM then discarded.")
                PrivacyItem(title = "Safety Hold Required", subtitle = "Hold Open Palm for 1s to activate, preventing accidental triggers.")
            }
        }
    }
}

@Composable
private fun PrivacyItem(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = HandlySuccess,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HandlyTextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = HandlyTextSecondary)
        }
    }
}

@Composable
private fun Step4CameraPermission(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(if (hasPermission) HandlySuccess.copy(alpha = 0.15f) else HandlyCyan.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = if (hasPermission) HandlySuccess else HandlyCyan,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Camera Permission",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = HandlyTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "The front camera is required to track hand gestures in real-time. No video or photos are ever saved or transmitted.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = HandlyTextSecondary,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (hasPermission) {
            Text(
                text = "✓ Camera permission granted!",
                color = HandlySuccess,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        } else {
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HandlyViolet),
                modifier = Modifier.testTag("allow_camera_button")
            ) {
                Text("Allow Camera", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Step5Accessibility(
    isConnected: Boolean,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(if (isConnected) HandlySuccess.copy(alpha = 0.15f) else HandlyViolet.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.AccessibilityNew,
                contentDescription = null,
                tint = if (isConnected) HandlySuccess else HandlyVioletBright,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Accessibility Service",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = HandlyTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Accessibility access is required so HANDLY can perform system actions (Home, Back, Recents, Scrolling) hands-free when you make gestures.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = HandlyTextSecondary,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isConnected) {
            Text(
                text = "✓ Accessibility Service connected!",
                color = HandlySuccess,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        } else {
            Button(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HandlyViolet),
                modifier = Modifier.testTag("enable_accessibility_button")
            ) {
                Text("Enable Accessibility", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tap button → Find 'HANDLY' → Toggle ON",
                fontSize = 12.sp,
                color = HandlyTextSecondary
            )
        }
    }
}

@Composable
private fun Step6Ready(
    isCameraReady: Boolean,
    isAccessibilityReady: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(HandlySuccess.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, HandlySuccess.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = HandlySuccess,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You're All Set!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = HandlyTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HANDLY is configured and ready for hands-free control.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = HandlyTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HandlySurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Camera Sensor:", fontSize = 13.sp, color = HandlyTextSecondary)
                    Text(
                        text = if (isCameraReady) "✓ Ready" else "⚠ Not Granted",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isCameraReady) HandlySuccess else HandlyWarning
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accessibility Service:", fontSize = 13.sp, color = HandlyTextSecondary)
                    Text(
                        text = if (isAccessibilityReady) "✓ Connected" else "⚠ Disabled",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isAccessibilityReady) HandlySuccess else HandlyWarning
                    )
                }
            }
        }
    }
}
