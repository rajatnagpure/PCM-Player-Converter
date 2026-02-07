package com.rajatnagpure.pcmplayerconverter.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.rajatnagpure.pcmplayerconverter.ui.components.AudioPlayerSheet
import com.rajatnagpure.pcmplayerconverter.ui.screens.ConverterScreen
import com.rajatnagpure.pcmplayerconverter.ui.screens.GeneratorScreen
import com.rajatnagpure.pcmplayerconverter.ui.viewmodel.MainViewModel

import androidx.navigation.NavGraph.Companion.findStartDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    intentRouteEvent: com.rajatnagpure.pcmplayerconverter.MainActivity.IntentRouteEvent? = null,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    var selectedItem by rememberSaveable { mutableStateOf(0) }
    
    // Handle intent routing reactively
    LaunchedEffect(intentRouteEvent) {
        intentRouteEvent?.let { event ->
            val route = event.route
            if (route.isNotEmpty() && route != "converter?uri={uri}") {
                selectedItem = if (route.contains("generator")) 1 else 0
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val currentFile by mainViewModel.currentFile.collectAsState()
    val progress by mainViewModel.progress.collectAsState()
    
    val items = listOf("Converter", "Generator")
    val icons = listOf(Icons.Filled.Audiotrack, Icons.Filled.GraphicEq)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PCM Converter") },
                actions = {
                    IconButton(onClick = { navController.navigate("help") }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline, 
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
                            selectedItem = index
                            
                            // Try to get uri from the current screen's SavedStateHandle via its backStackEntry
                            val currentBackStackEntry = navController.currentBackStackEntry
                            val currentUri = currentBackStackEntry?.arguments?.getString("uri")
                            
                            val route = if (index == 0) {
                                if (currentUri != null && currentUri != "{uri}" && currentUri != "null") "converter?uri=$currentUri" else "converter?uri={uri}"
                            } else {
                                if (currentUri != null && currentUri != "{uri}" && currentUri != "null") "generator?uri=$currentUri" else "generator?uri={uri}"
                            }
                            
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
                ) { ConverterScreen() }
                
                composable(
                    route = "generator?uri={uri}",
                    arguments = listOf(navArgument("uri") { type = NavType.StringType; nullable = true })
                ) { GeneratorScreen() }

                composable("help") { 
                    com.rajatnagpure.pcmplayerconverter.ui.NeedHelpScreen(
                        onBackClick = { navController.popBackStack() }
                    ) 
                }
            }
            
            AnimatedVisibility(
                visible = isPlaying && currentFile != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(innerPadding)
            ) {
                AudioPlayerSheet(
                    file = currentFile,
                    isPlaying = isPlaying,
                    progress = progress,
                    onProgressChange = { mainViewModel.seekTo(it) },
                    onStop = { mainViewModel.stopPlayback() }
                )
            }
        }
    }
}
