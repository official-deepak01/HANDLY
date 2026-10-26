package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TechCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & User Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HelpTopicCard(
                title = "1. Getting the Best Accuracy",
                content = "Hold your hand roughly 1 to 2 feet (30-60 cm) from the front camera. The distance guidance indicator in the Test Lab should show 'GOOD'. Ensure there is reasonable ambient lighting and avoid strong backlighting."
            )

            HelpTopicCard(
                title = "2. Master Safety Activation & Deactivation",
                content = "To prevent unintentional touches, control is OFF by default.\n• To Activate: Hold an Open Palm facing the camera for 1 second.\n• To Deactivate: Hold a tight Fist for 1 second.\n• Emergency Stop: Tap the red 'STOP CONTROL' button on the Home dashboard anytime."
            )

            HelpTopicCard(
                title = "3. Performing Swipes Reliably",
                content = "Make a deliberate, swift hand motion in the desired direction (Up, Down, Left, Right) and then pause. Each physical swipe produces at most one command. Stop your hand before initiating the next swipe."
            )

            HelpTopicCard(
                title = "4. Using Pinch to Tap",
                content = "Bring your thumb tip and index finger tip together until they touch. Gesture Control recognizes this as a Pinch. A tap is dispatched upon pinch contact, and repeated taps are suppressed while holding."
            )

            HelpTopicCard(
                title = "5. Automatic App Profile Switching",
                content = "When Automatic Profile Switching is enabled in Settings, the app automatically transitions to tailored gesture profiles when you open YouTube, TikTok, Instagram, Snapchat, or the Camera app."
            )

            HelpTopicCard(
                title = "6. Custom Profiles & Customization",
                content = "Visit the Profiles tab to create custom mappings for specific games or reading apps. In the Gestures tab, you can assign any gesture to navigation, scrolling, touch, media, or volume."
            )

            HelpTopicCard(
                title = "7. Troubleshooting Accessibility",
                content = "If navigation or scrolling does not respond, verify that the 'Gesture Control' Accessibility Service is toggled ON in Android Settings > Accessibility > Installed Apps."
            )

            HelpTopicCard(
                title = "8. Battery & Performance Optimization",
                content = "The vision pipeline automatically pauses image analysis whenever Gesture Control is stopped or when the screen is locked, ensuring minimal battery impact."
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HelpTopicCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = TechCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }
    }
}
