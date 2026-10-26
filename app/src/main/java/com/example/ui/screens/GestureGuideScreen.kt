package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionType
import com.example.model.GestureType
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.AlertRose
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TechIndigo

enum class GestureCategory(val label: String) {
    ALL("All Gestures"),
    STATIC("Static Poses"),
    DYNAMIC("Dynamic Swipes"),
    SAFETY("Safety Controls")
}

data class GestureGuideInfo(
    val gesture: GestureType,
    val category: GestureCategory,
    val icon: ImageVector,
    val defaultAction: ActionType,
    val howToPerform: String,
    val recognitionTips: String,
    val commonMistakes: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GestureGuideScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTest: (GestureType?) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(GestureCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val guides = remember { buildGestureGuides() }

    val filteredGuides = remember(selectedCategory, searchQuery) {
        guides.filter { guide ->
            val matchesCategory = when (selectedCategory) {
                GestureCategory.ALL -> true
                GestureCategory.STATIC -> guide.category == GestureCategory.STATIC || guide.category == GestureCategory.SAFETY
                GestureCategory.DYNAMIC -> guide.category == GestureCategory.DYNAMIC
                GestureCategory.SAFETY -> guide.category == GestureCategory.SAFETY
            }
            val matchesSearch = searchQuery.isBlank() ||
                    guide.gesture.displayName.contains(searchQuery, ignoreCase = true) ||
                    guide.defaultAction.displayName.contains(searchQuery, ignoreCase = true) ||
                    guide.howToPerform.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visual Gesture Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("guide_search_field"),
                placeholder = { Text("Search gestures or actions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TechCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GestureCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TechCyan.copy(alpha = 0.2f),
                            selectedLabelColor = TechCyan
                        )
                    )
                }
            }

            // Gesture Cards List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredGuides, key = { it.gesture.name }) { guide ->
                    GestureGuideCard(
                        guide = guide,
                        onTestGesture = { onNavigateToTest(guide.gesture) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun GestureGuideCard(
    guide: GestureGuideInfo,
    onTestGesture: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guide_card_${guide.gesture.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon + Name + Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(TechCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(guide.icon, contentDescription = null, tint = TechCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = guide.gesture.displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Default: ${guide.defaultAction.displayName}",
                            fontSize = 12.sp,
                            color = TechCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Category Badge
                val badgeColor = when (guide.category) {
                    GestureCategory.SAFETY -> AlertRose
                    GestureCategory.DYNAMIC -> TechIndigo
                    else -> ActiveEmerald
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (guide.category) {
                            GestureCategory.SAFETY -> "SAFETY"
                            GestureCategory.DYNAMIC -> "SWIPE"
                            else -> "STATIC"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            // How to Perform
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ActiveEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("How to Perform", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ActiveEmerald)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = guide.howToPerform,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }

            // Tips & Common Mistakes in 2 Columns or Rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tips
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ActiveEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Best Practice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ActiveEmerald)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = guide.recognitionTips,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }

                // Mistakes
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Avoid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = guide.commonMistakes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }

            // Action Button: Test in Lab
            Button(
                onClick = onTestGesture,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_in_lab_${guide.gesture.name.lowercase()}"),
                colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test in Lab", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun buildGestureGuides(): List<GestureGuideInfo> {
    return listOf(
        GestureGuideInfo(
            gesture = GestureType.OPEN_PALM,
            category = GestureCategory.SAFETY,
            icon = Icons.Default.PanTool,
            defaultAction = ActionType.NONE,
            howToPerform = "Face your open hand towards the front camera with all fingers extended. Hold still for 1 second to activate or resume gesture control.",
            recognitionTips = "Keep your palm perpendicular to the camera at 30-50 cm distance. Fingers comfortably spread.",
            commonMistakes = "Tilting palm away or dropping fingers too quickly before the 1-second hold completes."
        ),
        GestureGuideInfo(
            gesture = GestureType.FIST,
            category = GestureCategory.SAFETY,
            icon = Icons.Default.Lock,
            defaultAction = ActionType.BACK,
            howToPerform = "Curl all four fingers into your palm with thumb folded. In normal mode, it triggers Back. Hold steady for 1 second to safety-deactivate control.",
            recognitionTips = "Ensure all 4 fingers are tightly curled so the camera clearly sees the closed fist contour.",
            commonMistakes = "Leaving the index finger partially extended, which confuses with a pointing gesture."
        ),
        GestureGuideInfo(
            gesture = GestureType.INDEX_POINT,
            category = GestureCategory.STATIC,
            icon = Icons.Default.TouchApp,
            defaultAction = ActionType.TAP,
            howToPerform = "Extend your index finger upright towards the camera while keeping middle, ring, and pinky curled into your palm.",
            recognitionTips = "Directs the Air Cursor across screen. Maintain clear separation between index tip and curled fingers.",
            commonMistakes = "Pointing sideways at an angle or extending the middle finger alongside the index."
        ),
        GestureGuideInfo(
            gesture = GestureType.TWO_FINGERS,
            category = GestureCategory.STATIC,
            icon = Icons.Default.Fingerprint,
            defaultAction = ActionType.RECENTS,
            howToPerform = "Extend both index and middle fingers together or in a V-shape, with ring and pinky curled into the palm.",
            recognitionTips = "Clear space between index and middle fingers ensures rapid recognition. Default action opens Recent Apps.",
            commonMistakes = "Extending the thumb or curling the middle finger halfway."
        ),
        GestureGuideInfo(
            gesture = GestureType.PINCH,
            category = GestureCategory.STATIC,
            icon = Icons.Default.TouchApp,
            defaultAction = ActionType.TAP,
            howToPerform = "Bring the tip of your thumb and the tip of your index finger together so they touch. Dispatches an instant Tap at cursor position.",
            recognitionTips = "Keep other fingers relaxed. Contact of finger pads triggers the tap, with hold-suppression preventing repeat clicks.",
            commonMistakes = "Pinching behind another finger where the camera line-of-sight is obstructed."
        ),
        GestureGuideInfo(
            gesture = GestureType.OK_SIGN,
            category = GestureCategory.STATIC,
            icon = Icons.Default.CheckCircle,
            defaultAction = ActionType.HOME,
            howToPerform = "Form a circle with your thumb and index tips while extending middle, ring, and pinky fingers upright.",
            recognitionTips = "The extended three fingers create an unambiguous visual signature for instant Home navigation.",
            commonMistakes = "Dropping the three extended fingers, which resembles a simple pinch."
        ),
        GestureGuideInfo(
            gesture = GestureType.THUMBS_UP,
            category = GestureCategory.STATIC,
            icon = Icons.Default.ThumbUp,
            defaultAction = ActionType.VOLUME_UP,
            howToPerform = "Curl all 4 fingers into your palm and extend your thumb pointing straight up towards the ceiling.",
            recognitionTips = "Keep the hand upright with thumb clearly pointing above the wrist and index knuckles.",
            commonMistakes = "Tilting the hand sideways or leaving fingers loosely open."
        ),
        GestureGuideInfo(
            gesture = GestureType.THUMBS_DOWN,
            category = GestureCategory.STATIC,
            icon = Icons.Default.ThumbDown,
            defaultAction = ActionType.VOLUME_DOWN,
            howToPerform = "Curl all 4 fingers into your palm and extend your thumb pointing straight down towards the floor.",
            recognitionTips = "Keep thumb clearly pointed downwards below the knuckle line for volume decrement.",
            commonMistakes = "Thumb pointing toward the camera rather than downward."
        ),
        GestureGuideInfo(
            gesture = GestureType.SWIPE_UP,
            category = GestureCategory.DYNAMIC,
            icon = Icons.Default.ArrowUpward,
            defaultAction = ActionType.SCROLL_FORWARD,
            howToPerform = "Swipe your entire open hand swiftly in an upward motion (bottom to top) across the camera's view.",
            recognitionTips = "Make a single clean, continuous motion. The system confirms velocity and locks repeat triggers.",
            commonMistakes = "Moving too slowly (less than 40 cm/s) or returning the hand downward immediately without pause."
        ),
        GestureGuideInfo(
            gesture = GestureType.SWIPE_DOWN,
            category = GestureCategory.DYNAMIC,
            icon = Icons.Default.ArrowDownward,
            defaultAction = ActionType.SCROLL_BACKWARD,
            howToPerform = "Swipe your open hand smoothly downward (top to bottom) across the camera view.",
            recognitionTips = "Start near the top of the frame and move downward, then pause hand motion.",
            commonMistakes = "Starting too close to the bottom of the camera view where motion cannot be tracked."
        ),
        GestureGuideInfo(
            gesture = GestureType.SWIPE_LEFT,
            category = GestureCategory.DYNAMIC,
            icon = Icons.Default.Swipe,
            defaultAction = ActionType.NEXT_TRACK,
            howToPerform = "Swipe your hand horizontally from right to left across the camera field of view.",
            recognitionTips = "Ideal for media navigation (next track) or switching pages. Keep the palm open during swipe.",
            commonMistakes = "Curling the hand into a fist during the swipe."
        ),
        GestureGuideInfo(
            gesture = GestureType.SWIPE_RIGHT,
            category = GestureCategory.DYNAMIC,
            icon = Icons.Default.Swipe,
            defaultAction = ActionType.PREV_TRACK,
            howToPerform = "Swipe your hand horizontally from left to right across the camera field of view.",
            recognitionTips = "Triggers previous track or back page navigation. Maintain steady horizontal trajectory.",
            commonMistakes = "Diagonal motion that confuses vertical and horizontal swipe detectors."
        )
    )
}
