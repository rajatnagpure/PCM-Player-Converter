package com.rajatnagpure.pcmplayerconverter.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.rajatnagpure.pcmplayerconverter.ui.components.AudioPlayerSheet
import com.rajatnagpure.pcmplayerconverter.ui.screens.ConverterScreen
import com.rajatnagpure.pcmplayerconverter.ui.screens.GeneratorScreen
import com.rajatnagpure.pcmplayerconverter.ui.viewmodel.MainViewModel

import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    intentRouteEvent: com.rajatnagpure.pcmplayerconverter.MainActivity.IntentRouteEvent? = null,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    
    // Handle intent routing reactively
    LaunchedEffect(intentRouteEvent) {
        android.util.Log.d("AppNavigation", "LaunchedEffect triggered with event: $intentRouteEvent")
        intentRouteEvent?.let { event ->
            val route = event.route
            android.util.Log.d("AppNavigation", "Processing intent route: $route")
            
            // Validate route
            if (route.isNotBlank() && 
                route != "converter?uri={uri}" && 
                route != "generator?uri={uri}" &&
                !route.endsWith("uri=")) {
                
                try {
                    // Extract base route and uri param if present
                    val base = route.substringBefore("?")
                    val encodedParam = route.substringAfter("uri=", "")
                    // Decode once to ensure ViewModels receive the original URI string
                    val decodedUri = if (encodedParam.isNotBlank()) android.net.Uri.decode(encodedParam) else null

                    android.util.Log.d("AppNavigation", "Graph ready, navigating to: $route")
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }

                    // Wait for the back stack entry that matches the destination base route, then write the decoded URI
                    decodedUri?.let { dUri ->
                        val entry = navController.currentBackStackEntryFlow.first {
                            it.destination.route?.substringBefore("?") == base
                        }
                        entry.savedStateHandle.set("uri", dUri)
                        android.util.Log.d("AppNavigation", "Set savedStateHandle uri for $base -> $dUri")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppNavigation", "Navigation error for intent route", e)
                }
            } else {
                android.util.Log.d("AppNavigation", "Skipping invalid/placeholder intent route")
            }
        }
    }

    val isPlayerVisible by mainViewModel.isPlayerVisible.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val isPaused by mainViewModel.isPaused.collectAsState()
    val currentFile by mainViewModel.currentFile.collectAsState()
    val progress by mainViewModel.progress.collectAsState()
    
    // Get current route to determine if we should show the full-screen Help page
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoutePattern = navBackStackEntry?.destination?.route
    val isHelpScreen = currentRoutePattern == "help"

    // Derive selected tab from current route base (ignore query params)
    val currentRouteBase = currentRoutePattern?.substringBefore("?")
    val selectedItem = when (currentRouteBase) {
        "generator" -> 1
        else -> 0
    }

    if (isHelpScreen) {
        com.rajatnagpure.pcmplayerconverter.ui.NeedHelpScreen(
            onBackClick = { navController.popBackStack() }
        )
    } else {
        val items = listOf("Converter", "Generator")
        val icons = listOf(Icons.Filled.Audiotrack, Icons.Filled.GraphicEq)

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("PCM Converter") },
                    actions = {
                        IconButton(onClick = { navController.navigate("help") }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline, 
                                contentDescription = "Help",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.shadow(0.5.dp)
                )
            },
            bottomBar = {
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            ),
                            onClick = {
                                if (selectedItem != index) {
                                    // Try to get current uri to persist it when switching tabs
                                    val currentUri = navBackStackEntry?.arguments?.getString("uri")
                                    
                                    val route = if (index == 0) {
                                        if (!currentUri.isNullOrBlank() && currentUri != "{uri}" && currentUri != "null") "converter?uri=$currentUri" else "converter?uri={uri}"
                                    } else {
                                        if (!currentUri.isNullOrBlank() && currentUri != "{uri}" && currentUri != "null") "generator?uri=$currentUri" else "generator?uri={uri}"
                                    }
                                    
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = "converter?uri={uri}",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(
                        route = "converter?uri={uri}",
                        arguments = listOf(navArgument("uri") { type = NavType.StringType; nullable = true })
                    ) { backStackEntry -> ConverterScreen(backStackEntry) }
                    
                    composable(
                        route = "generator?uri={uri}",
                        arguments = listOf(navArgument("uri") { type = NavType.StringType; nullable = true })
                    ) { backStackEntry -> GeneratorScreen(backStackEntry) }

                    composable("help") { 
                        // Empty composable as it's handled outside the Scaffold
                    }
                }
                
                if (isPlayerVisible && currentFile != null) {
                    Dialog(onDismissRequest = { mainViewModel.dismissPlayer() }) {
                        AudioPlayerSheet(
                            file = currentFile,
                            progress = progress,
                            isPlaying = isPlaying,
                            isPaused = isPaused,
                            onProgressChange = { mainViewModel.seekTo(it) },
                            onTogglePlayback = { mainViewModel.togglePlayback() },
                            onDismiss = { mainViewModel.dismissPlayer() }
                        )
                    }
                }
            }
        }
    }
}
