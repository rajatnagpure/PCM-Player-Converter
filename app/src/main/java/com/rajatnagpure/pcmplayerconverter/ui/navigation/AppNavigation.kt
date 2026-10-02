package com.rajatnagpure.pcmplayerconverter.ui.navigation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.rajatnagpure.pcmplayerconverter.ui.viewmodel.PromoViewModel
import com.rajatnagpure.pcmplayerconverter.ui.components.PromoBanner
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsEvents
import com.rajatnagpure.pcmplayerconverter.config.AppConfig
import com.rajatnagpure.pcmplayerconverter.util.PlayStore

import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.flow.first

import com.rajatnagpure.pcmplayerconverter.ui.theme.neumorphism.neumorphic
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    intentRouteEvent: com.rajatnagpure.pcmplayerconverter.MainActivity.IntentRouteEvent? = null,
    mainViewModel: MainViewModel = hiltViewModel(),
    promoViewModel: PromoViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    // Manual screen_view tracking for Compose destinations
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            when (destination.route?.substringBefore("?")) {
                "converter" -> mainViewModel.trackScreen(AnalyticsEvents.SCREEN_CONVERTER)
                "generator" -> mainViewModel.trackScreen(AnalyticsEvents.SCREEN_GENERATOR)
                "help" -> mainViewModel.trackScreen(AnalyticsEvents.SCREEN_HELP)
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    
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

                    // On a cold start from a share intent this effect can run before NavHost has
                    // set its graph; wait for the first back stack entry instead of crashing/ignoring.
                    navController.currentBackStackEntryFlow.first()
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
    val promoVisible by promoViewModel.visible.collectAsState()

    // Re-check on every return to the app, e.g. from the Play Store after installing the game
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        promoViewModel.refresh()
    }

    LaunchedEffect(isPlayerVisible) {
        if (isPlayerVisible) mainViewModel.trackOverlay(AnalyticsEvents.SCREEN_PLAYER)
    }
    
    // Get current route to determine if we should show the full-screen Help page
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoutePattern = navBackStackEntry?.destination?.route
    val isHelpScreen = currentRoutePattern == "help"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.padding(end = 64.dp).neumorphic(cornerRadius = 16.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                ) {
                    val neuTheme = com.rajatnagpure.pcmplayerconverter.ui.theme.LocalNeuTheme.current
                    val drawerSecondaryColor = neuTheme.onSurface
                    Spacer(Modifier.height(32.dp))
                    com.rajatnagpure.pcmplayerconverter.ui.components.NeuCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.rajatnagpure.pcmplayerconverter.R.drawable.drawer_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                        append("PCM\n")
                                    }
                                    withStyle(style = SpanStyle(color = drawerSecondaryColor)) {
                                        append("CONVERTER")
                                    }
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    
                    val drawerItems = listOf(
                        DrawerItem("More Apps", "more_apps", Icons.Default.Apps) {
                            PlayStore.safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.DEVELOPER_PLAYSTORE_SEARCH_URL)))
                        },
                        DrawerItem("Share App", "share_app", Icons.Default.Share) {
                            PlayStore.safeStart(context, Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, AppConfig.APP_SHARE_TEXT_PREFIX)
                                type = "text/plain"
                            }.let { Intent.createChooser(it, null) })
                        },
                        DrawerItem("Request Feature", "feature_request", Icons.Default.BugReport) {
                            PlayStore.safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.FEATURE_REQUEST_FORM_URL)))
                        },
                        DrawerItem("Rate on Playstore", "rate", Icons.Default.Star) {
                            PlayStore.openListing(context, AppConfig.APP_PACKAGE)
                        },
                        DrawerItem("Contribute", "contribute", Icons.Default.Code) {
                            PlayStore.safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.GITHUB_REPO_URL)))
                        }
                    )

                    drawerItems.forEach { item ->
                        com.rajatnagpure.pcmplayerconverter.ui.components.AppButton(
                            text = item.label,
                            icon = item.icon,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                mainViewModel.logDrawerAction(item.analyticsId)
                                item.onClick()
                            },
                            iconColor = drawerSecondaryColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        ) {
            Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PCM ",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Converter",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        com.rajatnagpure.pcmplayerconverter.ui.components.AppIconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        com.rajatnagpure.pcmplayerconverter.ui.components.AppIconButton(onClick = { navController.navigate("help") }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook, 
                                contentDescription = "Education/Help",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        var showThemeDialog by remember { mutableStateOf(false) }
                        com.rajatnagpure.pcmplayerconverter.ui.components.AppIconButton(onClick = { showThemeDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings, 
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        if (showThemeDialog) {
                            LaunchedEffect(Unit) { mainViewModel.trackOverlay(AnalyticsEvents.SCREEN_SETTINGS) }
                            com.rajatnagpure.pcmplayerconverter.ui.components.ThemeSelectionDialog(
                                currentTheme = mainViewModel.currentTheme.collectAsState().value,
                                onThemeSelected = { mainViewModel.setTheme(it) },
                                onDismiss = { showThemeDialog = false },
                                onHapticsChanged = { mainViewModel.setHapticsEnabled(context, it) },
                                analyticsEnabled = mainViewModel.analyticsEnabled.collectAsState().value,
                                onAnalyticsChanged = { mainViewModel.setAnalyticsEnabled(it) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .neumorphic(cornerRadius = 24.dp)
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .neumorphic(cornerRadius = 24.dp),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    val navView = androidx.compose.ui.platform.LocalView.current
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = {
                                com.rajatnagpure.pcmplayerconverter.util.HapticsManager.perform(navView)
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
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                PromoBanner(
                    visible = promoVisible,
                    onPlayClick = {
                        promoViewModel.onClick()
                        PlayStore.openListing(context, AppConfig.FLOODFILL_PACKAGE, AppConfig.FLOODFILL_REFERRER)
                    },
                    onDismiss = { promoViewModel.onDismiss() }
                )
                NavHost(
                    navController = navController,
                    startDestination = "converter?uri={uri}",
                    modifier = Modifier.weight(1f)
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
}

private data class DrawerItem(
    val label: String,
    val analyticsId: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)
