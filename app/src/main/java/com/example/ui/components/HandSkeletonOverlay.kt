package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GestureType
import com.example.model.HandDistance
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
import com.example.vision.HandData

@Composable
fun HandSkeletonOverlay(
    handData: HandData,
    activeGesture: GestureType?,
    modifier: Modifier = Modifier,
    showHud: Boolean = true
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (handData.isPresent && handData.landmarks.size >= 21) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val points = handData.landmarks.map { p ->
                    Offset(p.x * w, p.y * h)
                }

                // 1. Draw Palm Region Polygon (Wrist 0, Thumb CMC 1, Index MCP 5, Middle MCP 9, Ring MCP 13, Pinky MCP 17)
                val palmPath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[1].x, points[1].y)
                    lineTo(points[5].x, points[5].y)
                    lineTo(points[9].x, points[9].y)
                    lineTo(points[13].x, points[13].y)
                    lineTo(points[17].x, points[17].y)
                    close()
                }

                val palmFillColor = when (activeGesture) {
                    GestureType.OPEN_PALM -> HandlySuccess.copy(alpha = 0.20f)
                    GestureType.FIST -> HandlyError.copy(alpha = 0.22f)
                    GestureType.PINCH -> HandlyViolet.copy(alpha = 0.24f)
                    null -> HandlyCyan.copy(alpha = 0.12f)
                    else -> HandlyVioletBright.copy(alpha = 0.18f)
                }
                drawPath(palmPath, color = palmFillColor, style = Fill)

                // 2. Bone connections for 21 landmarks
                val connections = listOf(
                    // Palm base
                    0 to 1, 0 to 5, 0 to 17, 5 to 9, 9 to 13, 13 to 17,
                    // Thumb
                    1 to 2, 2 to 3, 3 to 4,
                    // Index
                    5 to 6, 6 to 7, 7 to 8,
                    // Middle
                    9 to 10, 10 to 11, 11 to 12,
                    // Ring
                    13 to 14, 14 to 15, 15 to 16,
                    // Pinky
                    17 to 18, 18 to 19, 19 to 20
                )

                val boneColor = when (activeGesture) {
                    GestureType.OPEN_PALM -> HandlySuccess.copy(alpha = 0.92f)
                    GestureType.FIST -> HandlyError.copy(alpha = 0.92f)
                    GestureType.PINCH -> HandlyVioletBright.copy(alpha = 0.92f)
                    null -> HandlyViolet.copy(alpha = 0.80f)
                    else -> HandlyCyan.copy(alpha = 0.92f)
                }

                for ((startIdx, endIdx) in connections) {
                    if (startIdx < points.size && endIdx < points.size) {
                        drawLine(
                            color = boneColor,
                            start = points[startIdx],
                            end = points[endIdx],
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 3. Landmark Nodes
                val tipIndices = setOf(4, 8, 12, 16, 20)
                for (i in points.indices) {
                    val pt = points[i]
                    val isTip = i in tipIndices
                    val radius = if (isTip) 10f else 6f
                    val nodeColor = if (isTip) Color.White else HandlyCyan

                    // Outer dark rim for high contrast against camera background
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.6f),
                        radius = radius + 3f,
                        center = pt
                    )
                    // Inner glowing node
                    drawCircle(
                        color = nodeColor,
                        radius = radius,
                        center = pt
                    )
                }
            }
        } else {
            // Clean HANDLY guidance instruction when no hand is detected
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, HandlyViolet.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(HandlyViolet.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = "Show Hand",
                            tint = HandlyCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Show your hand to camera",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HANDLY tracking ready • 20–50 cm away",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // HUD Overlay
        if (showHud) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                // Tracking Status & Distance Pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hand Tracking Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (handData.isPresent) HandlySuccess else Color.Gray,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (handData.isPresent) "HAND DETECTED" else "SCANNING...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (handData.isPresent) HandlySuccess else Color.LightGray
                        )
                    }

                    // Distance Guidance Pill
                    if (handData.isPresent) {
                        val (distText, distColor) = when (handData.distance) {
                            HandDistance.GOOD -> "Hand position is good" to HandlySuccess
                            HandDistance.TOO_FAR -> "Move your hand closer" to HandlyWarning
                            HandDistance.TOO_CLOSE -> "Move your hand slightly back" to HandlyError
                            HandDistance.NO_HAND -> "" to Color.Transparent
                        }

                        if (distText.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = distText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = distColor
                                )
                            }
                        }
                    }
                }

                // Detected Gesture & Confidence Badge
                if (handData.isPresent && activeGesture != null) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeGesture.displayName.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HandlyCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val confPercent = (handData.confidence * 100).toInt()
                        Text(
                            text = "$confPercent% conf",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
