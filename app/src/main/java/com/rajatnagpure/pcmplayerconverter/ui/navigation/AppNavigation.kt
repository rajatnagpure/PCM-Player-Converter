package com.rajatnagpure.pcmplayerconverter.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
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
    startDestination: String = "converter?uri={uri}",
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    var selectedItem by rememberSaveable { 
        mutableStateOf(if (startDestination.startsWith("generator")) 1 else 0) 
    }
    
    LaunchedEffect(startDestination) {
        selectedItem = if (startDestination.startsWith("generator")) 1 else 0
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.shadow(8.dp)
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            val route = if (index == 0) "converter?uri={uri}" else "generator?uri={uri}"
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
                startDestination = "converter?uri={uri}", // Use argument-based route as start
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
