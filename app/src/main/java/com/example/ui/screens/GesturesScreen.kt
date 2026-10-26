package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionCategory
import com.example.model.ActionType
import com.example.model.GestureType
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
import com.example.viewmodel.GestureControlViewModel

@Composable
fun GesturesScreen(
    viewModel: GestureControlViewModel,
    onNavigateToGuide: () -> Unit = {}
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    var selectedGestureForEdit by remember { mutableStateOf<GestureType?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val allGestures = GestureType.values().toList()
    val filteredGestures = when (selectedFilter) {
        "STATIC" -> allGestures.filter { !it.isDynamic }
        "SWIPES" -> allGestures.filter { it.isDynamic }
        else -> allGestures
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ==========================================
        // 1. HEADER (Section 7)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Your gestures",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = HandlyTextPrimary
                )
                Text(
                    text = "Simple movements. Powerful controls.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HandlyVioletBright,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Guide Quick Button
            IconButton(
                onClick = onNavigateToGuide,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(HandlyElevated)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Visual Gesture Guide",
                    tint = HandlyCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter chips (All, Static, Swipes)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All (${allGestures.size})", "STATIC" to "Static Poses", "SWIPES" to "Swipe Motions").forEach { (filterKey, label) ->
                val isSelected = selectedFilter == filterKey
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filterKey },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HandlyViolet,
                        selectedLabelColor = Color.White,
                        containerColor = HandlyElevated,
                        labelColor = HandlyTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) HandlyViolet else HandlySurface,
                        borderWidth = 1.dp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Profile Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HandlySurface)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Profile: ${activeProfile.name}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = HandlyTextSecondary
            )
            Text(
                text = "Tap any card to assign action",
                fontSize = 11.sp,
                color = HandlyCyan
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // 2. GESTURE CARDS LIST (Section 7 & 8)
        // ==========================================
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredGestures) { gesture ->
                val mapping = activeProfile.mappings[gesture]
                val action = mapping?.action ?: ActionType.NONE
                val isEnabled = mapping?.isEnabled ?: true

                GestureCardItem(
                    gesture = gesture,
                    action = action,
                    isEnabled = isEnabled,
                    onToggleEnabled = { enabled ->
                        viewModel.updateGestureMapping(
                            profileId = activeProfile.id,
                            gesture = gesture,
                            action = action,
                            isEnabled = enabled
                        )
                    },
                    onEditClick = {
                        selectedGestureForEdit = gesture
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ==========================================
    // ACTION ASSIGNMENT DIALOG
    // ==========================================
    selectedGestureForEdit?.let { gesture ->
        val currentMapping = activeProfile.mappings[gesture]
        val currentAction = currentMapping?.action ?: ActionType.NONE

        AlertDialog(
            onDismissRequest = { selectedGestureForEdit = null },
            containerColor = HandlySurface,
            titleContentColor = HandlyTextPrimary,
            textContentColor = HandlyTextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HandGestureIllustration(
                        gesture = gesture,
                        size = 38.dp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = "Assign Action",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = gesture.displayName,
                            fontSize = 13.sp,
                            color = HandlyVioletBright
                        )
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    val groupedActions = ActionType.values().groupBy { it.category }

                    groupedActions.forEach { (category, actions) ->
                        item {
                            Text(
                                text = category.displayName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HandlyCyan,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                            )
                        }

                        items(actions) { action ->
                            val isSelected = action == currentAction
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) HandlyViolet.copy(alpha = 0.20f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.updateGestureMapping(
                                            profileId = activeProfile.id,
                                            gesture = gesture,
                                            action = action,
                                            isEnabled = currentMapping?.isEnabled ?: true
                                        )
                                        selectedGestureForEdit = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = action.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) HandlyVioletBright else HandlyTextPrimary
                                    )
                                    Text(
                                        text = action.description,
                                        fontSize = 11.sp,
                                        color = HandlyTextSecondary
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = HandlySuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedGestureForEdit = null }) {
                    Text("Close", color = HandlyVioletBright, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Visually distinct gesture card explicitly presenting:
 * [HAND ILLUSTRATION]
 * Gesture Name (e.g. Swipe Up)
 * Mapped Action (e.g. Home)
 * Movement instruction (e.g. Move your hand upward)
 * Clear Gesture -> Action relationship visible immediately.
 */
@Composable
private fun GestureCardItem(
    gesture: GestureType,
    action: ActionType,
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEditClick: () -> Unit
) {
    val description = when (gesture) {
        GestureType.OPEN_PALM -> "Show an open palm towards the camera"
        GestureType.FIST -> "Close all fingers into a tight fist"
        GestureType.INDEX_POINT -> "Point with your index finger"
        GestureType.TWO_FINGERS -> "Hold index and middle fingers in a V shape"
        GestureType.THUMBS_UP -> "Extend your thumb upward with closed fist"
        GestureType.THUMBS_DOWN -> "Extend your thumb downward with closed fist"
        GestureType.PINCH -> "Bring thumb and index fingertips together"
        GestureType.OK_SIGN -> "Touch thumb and index in an O ring, 3 fingers up"
        GestureType.SWIPE_LEFT -> "Move your hand from right to left"
        GestureType.SWIPE_RIGHT -> "Move your hand from left to right"
        GestureType.SWIPE_UP -> "Move your hand upward"
        GestureType.SWIPE_DOWN -> "Move your hand downward"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
            .testTag("gesture_card_${gesture.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HandlySurface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isEnabled) HandlyViolet.copy(alpha = 0.22f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Hand Illustration (Unique vector per gesture)
            HandGestureIllustration(
                gesture = gesture,
                size = 64.dp,
                tint = if (isEnabled) HandlyCyan else HandlyTextSecondary
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Center: Gesture -> Action information
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = gesture.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = HandlyTextPrimary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Motion or Pose badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (gesture.isDynamic) HandlyViolet.copy(alpha = 0.20f)
                                else HandlyCyan.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (gesture.isDynamic) "SWIPE" else "POSE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gesture.isDynamic) HandlyVioletBright else HandlyCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Gesture -> Action relationship clearly visible immediately (Section 8)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HandlyElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Action: ",
                        fontSize = 11.sp,
                        color = HandlyTextSecondary
                    )
                    Text(
                        text = if (isEnabled) action.displayName else "Disabled",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled && action != ActionType.NONE) HandlySuccess else HandlyTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Instructional text
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = HandlyTextSecondary,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Edit Button and Toggle Switch
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Action",
                        tint = HandlyVioletBright,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HandlySuccess,
                        checkedTrackColor = HandlySuccess.copy(alpha = 0.35f),
                        uncheckedThumbColor = HandlyTextSecondary,
                        uncheckedTrackColor = HandlyElevated
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
