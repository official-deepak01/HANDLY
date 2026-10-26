package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.VisualFeedbackOverlay
import com.example.ui.screens.AccessibilityDisclosureScreen
import com.example.ui.screens.AirCursorCalibrationScreen
import com.example.ui.screens.AirCursorScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.GestureGuideScreen
import com.example.ui.screens.GestureTrainingScreen
import com.example.ui.screens.GesturesScreen
import com.example.ui.screens.HelpScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TestScreen
import com.example.ui.theme.HandlyElevated
import com.example.ui.theme.HandlySurface
import com.example.ui.theme.HandlyTextPrimary
import com.example.ui.theme.HandlyTextSecondary
import com.example.ui.theme.HandlyViolet
import com.example.viewmodel.GestureControlViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val GESTURES = "gestures"
    const val PROFILES = "profiles"
    const val TEST = "test"
    const val SETTINGS = "settings"
    const val TRAINING = "training"
    const val HELP = "help"
    const val GUIDE = "guide"
    const val PRIVACY = "privacy"
    const val ACCESSIBILITY_DISCLOSURE = "accessibility_disclosure"
    const val AIR_CURSOR = "air_cursor"
    const val AIR_CURSOR_CALIBRATION = "air_cursor_calibration"
    const val DIAGNOSTICS = "diagnostics"
    const val HISTORY = "history"
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector, val tag: String) {
    object Home : BottomNavItem(Routes.HOME, "Home", Icons.Default.Home, "nav_home")
    object Gestures : BottomNavItem(Routes.GESTURES, "Gestures", Icons.Default.PanTool, "nav_gestures")
    object Control : BottomNavItem(Routes.TEST, "Control", Icons.Default.TouchApp, "nav_control")
    object Settings : BottomNavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings, "nav_settings")
}

@Composable
fun MainNavigation(viewModel: GestureControlViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val visualFeedback by viewModel.visualFeedback.collectAsState()

    // 4 destinations matching the prompt: Home, Gestures, Control, Settings
    val navItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Gestures,
        BottomNavItem.Control,
        BottomNavItem.Settings
    )

    val showNavBar = currentRoute in navItems.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        if (isExpandedScreen && showNavBar) {
            // Adaptive Navigation Rail for larger screens / foldables / tablets
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = HandlySurface,
                    contentColor = HandlyTextPrimary
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = HandlyViolet,
                                selectedTextColor = HandlyViolet,
                                indicatorColor = HandlyViolet.copy(alpha = 0.20f),
                                unselectedIconColor = HandlyTextSecondary,
                                unselectedTextColor = HandlyTextSecondary
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    AppNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        isOnboardingCompleted = isOnboardingCompleted
                    )
                    VisualFeedbackOverlay(feedback = visualFeedback)
                }
            }
        } else {
            // Standard Phone Bottom Navigation
            Scaffold(
                bottomBar = {
                    if (showNavBar) {
                        NavigationBar(
                            containerColor = HandlySurface,
                            contentColor = HandlyTextPrimary
                        ) {
                            navItems.forEach { item ->
                                val isSelected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = HandlyViolet,
                                        selectedTextColor = HandlyViolet,
                                        indicatorColor = HandlyViolet.copy(alpha = 0.20f),
                                        unselectedIconColor = HandlyTextSecondary,
                                        unselectedTextColor = HandlyTextSecondary
                                    ),
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AppNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        isOnboardingCompleted = isOnboardingCompleted
                    )
                    VisualFeedbackOverlay(feedback = visualFeedback)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: GestureControlViewModel,
    isOnboardingCompleted: Boolean
) {
    val startDestination = if (isOnboardingCompleted) Routes.HOME else Routes.ONBOARDING

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToTest = { navController.navigate(Routes.TEST) },
                onNavigateToGestures = { navController.navigate(Routes.GESTURES) },
                onNavigateToProfiles = { navController.navigate(Routes.PROFILES) },
                onNavigateToTraining = { navController.navigate(Routes.TRAINING) },
                onNavigateToAirCursor = { navController.navigate(Routes.AIR_CURSOR) },
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToGuide = { navController.navigate(Routes.GUIDE) }
            )
        }

        composable(Routes.GESTURES) {
            GesturesScreen(
                viewModel = viewModel,
                onNavigateToGuide = { navController.navigate(Routes.GUIDE) }
            )
        }

        composable(Routes.PROFILES) {
            ProfilesScreen(viewModel = viewModel)
        }

        composable(Routes.TEST) {
            TestScreen(viewModel = viewModel)
        }

        composable(Routes.GUIDE) {
            GestureGuideScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTest = {
                    navController.navigate(Routes.TEST)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToPrivacy = { navController.navigate(Routes.PRIVACY) },
                onNavigateToAccessibilityDisclosure = { navController.navigate(Routes.ACCESSIBILITY_DISCLOSURE) },
                onNavigateToHelp = { navController.navigate(Routes.HELP) },
                onNavigateToTraining = { navController.navigate(Routes.TRAINING) },
                onNavigateToAirCursor = { navController.navigate(Routes.AIR_CURSOR) },
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) }
            )
        }

        composable(Routes.AIR_CURSOR) {
            AirCursorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCalibration = { navController.navigate(Routes.AIR_CURSOR_CALIBRATION) }
            )
        }

        composable(Routes.AIR_CURSOR_CALIBRATION) {
            AirCursorCalibrationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TRAINING) {
            GestureTrainingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HELP) {
            HelpScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.ACCESSIBILITY_DISCLOSURE) {
            AccessibilityDisclosureScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
