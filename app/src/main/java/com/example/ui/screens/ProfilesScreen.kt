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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.model.ActionType
import com.example.model.GestureProfile
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.AlertRose
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TechIndigo
import com.example.viewmodel.GestureControlViewModel

@Composable
fun ProfilesScreen(viewModel: GestureControlViewModel) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfileDesc by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = TechCyan,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("create_profile_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Profiles",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Switch profiles manually or automatically per app",
                fontSize = 13.sp,
                color = TechCyan,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allProfiles, key = { it.id }) { profile ->
                    val isActive = profile.id == activeProfileId
                    ProfileCard(
                        profile = profile,
                        isActive = isActive,
                        onActivate = { viewModel.selectProfile(profile.id) },
                        onDuplicate = { viewModel.duplicateProfile(profile.id) },
                        onReset = { viewModel.resetProfile(profile.id) },
                        onDelete = { viewModel.deleteCustomProfile(profile.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Custom Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newProfileDesc,
                        onValueChange = { newProfileDesc = it },
                        label = { Text("Description") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            viewModel.createCustomProfile(
                                name = newProfileName.trim(),
                                description = newProfileDesc.trim()
                            )
                            newProfileName = ""
                            newProfileDesc = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: GestureProfile,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDuplicate: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val activeMappingsCount = profile.mappings.values.count { it.isEnabled && it.action != ActionType.NONE }

    val borderColor = if (isActive) ActiveEmerald.copy(alpha = 0.6f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onActivate() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) ActiveEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (isActive) ActiveEmerald.copy(alpha = 0.18f) else TechIndigo.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = if (isActive) ActiveEmerald else TechIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (profile.isBuiltIn) MaterialTheme.colorScheme.surface
                                        else TechCyan.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (profile.isBuiltIn) "BUILT-IN" else "CUSTOM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profile.isBuiltIn) MaterialTheme.colorScheme.onSurfaceVariant else TechCyan
                                )
                            }
                        }

                        Text(
                            text = "$activeMappingsCount active gesture actions",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )

                        if (profile.isBuiltIn) {
                            DropdownMenuItem(
                                text = { Text("Reset to Default") },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                onClick = {
                                    menuExpanded = false
                                    onReset()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Delete Profile", color = AlertRose) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = AlertRose) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            if (profile.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profile.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.packageName != null) {
                    Text(
                        text = "Auto-link: ${profile.packageName}",
                        fontSize = 11.sp,
                        color = TechCyan
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (isActive) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ActiveEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ActiveEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ActiveEmerald)
                    }
                } else {
                    Button(
                        onClick = onActivate,
                        colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Activate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
