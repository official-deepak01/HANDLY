package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.GestureType
import com.example.ui.theme.HandlyCyan
import com.example.ui.theme.HandlyElevated
import com.example.ui.theme.HandlySuccess
import com.example.ui.theme.HandlyViolet
import com.example.ui.theme.HandlyVioletBright

/**
 * Dedicated visual vector illustrations for every supported hand gesture.
 * Provides distinct, non-generic hand shapes and motion trails for all 12 gestures.
 */
@Composable
fun HandGestureIllustration(
    gesture: GestureType,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    tint: Color = HandlyCyan
) {
    Box(
        modifier = modifier
            .size(size)
            .background(HandlyElevated, RoundedCornerShape(16.dp))
            .border(1.dp, HandlyViolet.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.75f)) {
            val w = this.size.width
            val h = this.size.height

            when (gesture) {
                GestureType.OPEN_PALM -> drawOpenPalm(w, h, tint)
                GestureType.FIST -> drawFist(w, h, tint)
                GestureType.INDEX_POINT -> drawIndexPoint(w, h, tint)
                GestureType.TWO_FINGERS -> drawPeace(w, h, tint)
                GestureType.THUMBS_UP -> drawThumbsUp(w, h, tint)
                GestureType.THUMBS_DOWN -> drawThumbsDown(w, h, tint)
                GestureType.PINCH -> drawPinch(w, h, tint)
                GestureType.OK_SIGN -> drawOkSign(w, h, tint)
                GestureType.SWIPE_LEFT -> drawSwipeTrail(w, h, tint, direction = Direction.LEFT)
                GestureType.SWIPE_RIGHT -> drawSwipeTrail(w, h, tint, direction = Direction.RIGHT)
                GestureType.SWIPE_UP -> drawSwipeTrail(w, h, tint, direction = Direction.UP)
                GestureType.SWIPE_DOWN -> drawSwipeTrail(w, h, tint, direction = Direction.DOWN)
            }
        }
    }
}

private enum class Direction { LEFT, RIGHT, UP, DOWN }

// 1. OPEN PALM: All 5 extended fingers spreading outward from palm
private fun DrawScope.drawOpenPalm(w: Float, h: Float, color: Color) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Palm base
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(w * 0.25f, h * 0.48f),
        size = Size(w * 0.50f, h * 0.42f),
        cornerRadius = CornerRadius(10f, 10f),
        style = Fill
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.25f, h * 0.48f),
        size = Size(w * 0.50f, h * 0.42f),
        cornerRadius = CornerRadius(10f, 10f),
        style = stroke
    )
    // Thumb
    drawLine(color, Offset(w * 0.25f, h * 0.65f), Offset(w * 0.08f, h * 0.45f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.08f, h * 0.45f), Offset(w * 0.12f, h * 0.36f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    // Index
    drawLine(color, Offset(w * 0.33f, h * 0.48f), Offset(w * 0.33f, h * 0.12f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    // Middle
    drawLine(color, Offset(w * 0.47f, h * 0.48f), Offset(w * 0.47f, h * 0.08f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    // Ring
    drawLine(color, Offset(w * 0.61f, h * 0.48f), Offset(w * 0.61f, h * 0.14f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    // Pinky
    drawLine(color, Offset(w * 0.74f, h * 0.48f), Offset(w * 0.76f, h * 0.24f), strokeWidth = 3.5f, cap = StrokeCap.Round)
}

// 2. FIST: Curled compact fingers with thumb locked across front
private fun DrawScope.drawFist(w: Float, h: Float, color: Color) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Solid fist base
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(w * 0.20f, h * 0.30f),
        size = Size(w * 0.60f, h * 0.55f),
        cornerRadius = CornerRadius(14f, 14f),
        style = Fill
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.20f, h * 0.30f),
        size = Size(w * 0.60f, h * 0.55f),
        cornerRadius = CornerRadius(14f, 14f),
        style = stroke
    )
    // Curled knuckle lines
    drawLine(color, Offset(w * 0.35f, h * 0.30f), Offset(w * 0.35f, h * 0.52f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.50f, h * 0.30f), Offset(w * 0.50f, h * 0.52f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.65f, h * 0.30f), Offset(w * 0.65f, h * 0.52f), strokeWidth = 3f, cap = StrokeCap.Round)
    // Thumb across the front
    val thumbPath = Path().apply {
        moveTo(w * 0.18f, h * 0.68f)
        lineTo(w * 0.38f, h * 0.58f)
        lineTo(w * 0.65f, h * 0.62f)
    }
    drawPath(thumbPath, color = color, style = stroke)
}

// 3. INDEX POINT: Single index finger pointing up, others curled
private fun DrawScope.drawIndexPoint(w: Float, h: Float, color: Color) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Fist base
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(w * 0.25f, h * 0.42f),
        size = Size(w * 0.55f, h * 0.48f),
        cornerRadius = CornerRadius(12f, 12f),
        style = Fill
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.25f, h * 0.42f),
        size = Size(w * 0.55f, h * 0.48f),
        cornerRadius = CornerRadius(12f, 12f),
        style = stroke
    )
    // Pointing Index finger
    drawLine(color, Offset(w * 0.38f, h * 0.42f), Offset(w * 0.38f, h * 0.10f), strokeWidth = 4f, cap = StrokeCap.Round)
    // Touch beacon at the tip
    drawCircle(color = HandlyVioletBright, radius = 4f, center = Offset(w * 0.38f, h * 0.10f))
    // Folded knuckles
    drawLine(color, Offset(w * 0.54f, h * 0.42f), Offset(w * 0.54f, h * 0.60f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.68f, h * 0.42f), Offset(w * 0.68f, h * 0.60f), strokeWidth = 3f, cap = StrokeCap.Round)
}

// 4. PEACE / TWO FINGERS: V-shape with Index and Middle extended
private fun DrawScope.drawPeace(w: Float, h: Float, color: Color) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Palm base
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(w * 0.26f, h * 0.45f),
        size = Size(w * 0.52f, h * 0.45f),
        cornerRadius = CornerRadius(12f, 12f),
        style = Fill
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.26f, h * 0.45f),
        size = Size(w * 0.52f, h * 0.45f),
        cornerRadius = CornerRadius(12f, 12f),
        style = stroke
    )
    // Index finger angled left
    drawLine(color, Offset(w * 0.42f, h * 0.45f), Offset(w * 0.28f, h * 0.12f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    // Middle finger angled right
    drawLine(color, Offset(w * 0.58f, h * 0.45f), Offset(w * 0.72f, h * 0.12f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    // Folded thumb and ring/pinky
    drawLine(color, Offset(w * 0.26f, h * 0.64f), Offset(w * 0.48f, h * 0.64f), strokeWidth = 3f, cap = StrokeCap.Round)
}

// 5. THUMBS UP: Closed fist with upright thumb pointing up
private fun DrawScope.drawThumbsUp(w: Float, h: Float, color: Color) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Fist base
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(w * 0.25f, h * 0.38f),
        size = Size(w * 0.58f, h * 0.50f),
        cornerRadius = CornerRadius(12f, 12f),
        style = Fill
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.25f, h * 0.38f),
        size = Size(w * 0.58f, h * 0.50f),
        cornerRadius = CornerRadius(12f, 12f),
        style = stroke
    )
    // Upright thumb pointing high
    val thumbPath = Path().apply {
        moveTo(w * 0.32f, h * 0.45f)
        lineTo(w * 0.24f, h * 0.25f)
        lineTo(w * 0.24f, h * 0.10f)
        lineTo(w * 0.34f, h * 0.10f)
        lineTo(w * 0.40f, h * 0.38f)
    }
    drawPath(thumbPath, color = color, style = stroke)
    // Knuckle creases
    drawLine(color, Offset(w * 0.50f, h * 0.42f), Offset(w * 0.76f, h * 0.42f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.50f, h * 0.55f), Offset(w * 0.76f, h * 0.55f), strokeWidth = 2.5f, cap = StrokeCap.Round)
}

// 6. THUMBS DOWN: Closed fist with thumb pointed straight down
private fun DrawScope.drawThumbsDown(w: Float, h: Float, color: Color) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Fist base
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(w * 0.25f, h * 0.12f),
        size = Size(w * 0.58f, h * 0.50f),
        cornerRadius = CornerRadius(12f, 12f),
        style = Fill
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.25f, h * 0.12f),
        size = Size(w * 0.58f, h * 0.50f),
        cornerRadius = CornerRadius(12f, 12f),
        style = stroke
    )
    // Downward thumb
    val thumbPath = Path().apply {
        moveTo(w * 0.32f, h * 0.55f)
        lineTo(w * 0.24f, h * 0.75f)
        lineTo(w * 0.24f, h * 0.90f)
        lineTo(w * 0.34f, h * 0.90f)
        lineTo(w * 0.40f, h * 0.62f)
    }
    drawPath(thumbPath, color = color, style = stroke)
    // Knuckle creases
    drawLine(color, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.76f, h * 0.28f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.50f, h * 0.42f), Offset(w * 0.76f, h * 0.42f), strokeWidth = 2.5f, cap = StrokeCap.Round)
}

// 7. PINCH: Thumb tip and index tip touching closely
private fun DrawScope.drawPinch(w: Float, h: Float, color: Color) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Palm base
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(w * 0.35f, h * 0.48f),
        size = Size(w * 0.45f, h * 0.42f),
        cornerRadius = CornerRadius(10f, 10f),
        style = Fill
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.35f, h * 0.48f),
        size = Size(w * 0.45f, h * 0.42f),
        cornerRadius = CornerRadius(10f, 10f),
        style = stroke
    )
    // Thumb arc meeting index tip
    val thumbPath = Path().apply {
        moveTo(w * 0.35f, h * 0.68f)
        cubicTo(w * 0.15f, h * 0.62f, w * 0.15f, h * 0.36f, w * 0.35f, h * 0.30f)
    }
    drawPath(thumbPath, color = color, style = stroke)
    // Index arc meeting thumb tip
    val indexPath = Path().apply {
        moveTo(w * 0.48f, h * 0.48f)
        cubicTo(w * 0.48f, h * 0.22f, w * 0.38f, h * 0.22f, w * 0.35f, h * 0.30f)
    }
    drawPath(indexPath, color = color, style = stroke)
    // Pinch contact point glow
    drawCircle(color = HandlySuccess, radius = 5f, center = Offset(w * 0.35f, h * 0.30f))
}

// 8. OK SIGN: Thumb and index forming a circle, other 3 fingers extended
private fun DrawScope.drawOkSign(w: Float, h: Float, color: Color) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // "O" circle formed by thumb & index
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = w * 0.18f,
        center = Offset(w * 0.36f, h * 0.48f),
        style = Fill
    )
    drawCircle(
        color = color,
        radius = w * 0.18f,
        center = Offset(w * 0.36f, h * 0.48f),
        style = stroke
    )
    // Palm base
    drawLine(color, Offset(w * 0.44f, h * 0.64f), Offset(w * 0.68f, h * 0.88f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    // 3 extended fingers (Middle, Ring, Pinky) pointing upwards
    drawLine(color, Offset(w * 0.52f, h * 0.48f), Offset(w * 0.58f, h * 0.10f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.60f, h * 0.54f), Offset(w * 0.72f, h * 0.14f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.68f, h * 0.60f), Offset(w * 0.84f, h * 0.24f), strokeWidth = 3.5f, cap = StrokeCap.Round)
}

// 9-12. SWIPES: Hand contour with distinct directional speed trails and arrows
private fun DrawScope.drawSwipeTrail(w: Float, h: Float, color: Color, direction: Direction) {
    val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Mini open hand silhouette
    val handCenter = when (direction) {
        Direction.LEFT -> Offset(w * 0.62f, h * 0.50f)
        Direction.RIGHT -> Offset(w * 0.38f, h * 0.50f)
        Direction.UP -> Offset(w * 0.50f, h * 0.66f)
        Direction.DOWN -> Offset(w * 0.50f, h * 0.34f)
    }

    drawCircle(color = color.copy(alpha = 0.25f), radius = w * 0.22f, center = handCenter, style = Fill)
    drawCircle(color = color, radius = w * 0.22f, center = handCenter, style = stroke)

    // Motion arrows and speed lines
    when (direction) {
        Direction.LEFT -> {
            drawLine(HandlyVioletBright, Offset(w * 0.45f, h * 0.35f), Offset(w * 0.12f, h * 0.35f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(HandlyVioletBright, Offset(w * 0.45f, h * 0.50f), Offset(w * 0.08f, h * 0.50f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(HandlyVioletBright, Offset(w * 0.45f, h * 0.65f), Offset(w * 0.12f, h * 0.65f), strokeWidth = 3f, cap = StrokeCap.Round)
            // Arrowhead
            val arrow = Path().apply {
                moveTo(w * 0.18f, h * 0.42f)
                lineTo(w * 0.08f, h * 0.50f)
                lineTo(w * 0.18f, h * 0.58f)
            }
            drawPath(arrow, color = HandlyVioletBright, style = stroke)
        }
        Direction.RIGHT -> {
            drawLine(HandlyVioletBright, Offset(w * 0.55f, h * 0.35f), Offset(w * 0.88f, h * 0.35f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(HandlyVioletBright, Offset(w * 0.55f, h * 0.50f), Offset(w * 0.92f, h * 0.50f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(HandlyVioletBright, Offset(w * 0.55f, h * 0.65f), Offset(w * 0.88f, h * 0.65f), strokeWidth = 3f, cap = StrokeCap.Round)
            // Arrowhead
            val arrow = Path().apply {
                moveTo(w * 0.82f, h * 0.42f)
                lineTo(w * 0.92f, h * 0.50f)
                lineTo(w * 0.82f, h * 0.58f)
            }
            drawPath(arrow, color = HandlyVioletBright, style = stroke)
        }
        Direction.UP -> {
            drawLine(HandlyVioletBright, Offset(w * 0.35f, h * 0.45f), Offset(w * 0.35f, h * 0.14f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(HandlyVioletBright, Offset(w * 0.50f, h * 0.45f), Offset(w * 0.50f, h * 0.08f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(HandlyVioletBright, Offset(w * 0.65f, h * 0.45f), Offset(w * 0.65f, h * 0.14f), strokeWidth = 3f, cap = StrokeCap.Round)
            // Arrowhead
            val arrow = Path().apply {
                moveTo(w * 0.42f, h * 0.18f)
                lineTo(w * 0.50f, h * 0.08f)
                lineTo(w * 0.58f, h * 0.18f)
            }
            drawPath(arrow, color = HandlyVioletBright, style = stroke)
        }
        Direction.DOWN -> {
            drawLine(HandlyVioletBright, Offset(w * 0.35f, h * 0.55f), Offset(w * 0.35f, h * 0.86f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(HandlyVioletBright, Offset(w * 0.50f, h * 0.55f), Offset(w * 0.50f, h * 0.92f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(HandlyVioletBright, Offset(w * 0.65f, h * 0.55f), Offset(w * 0.65f, h * 0.86f), strokeWidth = 3f, cap = StrokeCap.Round)
            // Arrowhead
            val arrow = Path().apply {
                moveTo(w * 0.42f, h * 0.82f)
                lineTo(w * 0.50f, h * 0.92f)
                lineTo(w * 0.58f, h * 0.82f)
            }
            drawPath(arrow, color = HandlyVioletBright, style = stroke)
        }
    }
}
