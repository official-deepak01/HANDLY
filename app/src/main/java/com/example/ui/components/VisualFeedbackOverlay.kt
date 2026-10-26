package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionResult
import com.example.model.ExecutionFeedback
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.AlertRose
import com.example.ui.theme.AmberWarning

@Composable
fun VisualFeedbackOverlay(
    feedback: ExecutionFeedback?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = feedback != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            if (feedback != null) {
                val isSuccess = feedback.result == ActionResult.SUCCESS
                val isWarning = feedback.result == ActionResult.SERVICE_DISABLED || feedback.result == ActionResult.UNSUPPORTED
                val bgColor = when {
                    isSuccess -> ActiveEmerald.copy(alpha = 0.92f)
                    isWarning -> AmberWarning.copy(alpha = 0.92f)
                    else -> AlertRose.copy(alpha = 0.92f)
                }

                Row(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when {
                        isSuccess -> Icons.Default.CheckCircle
                        isWarning -> Icons.Default.Info
                        else -> Icons.Default.ErrorOutline
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${feedback.gesture.displayName}: ${feedback.message}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
