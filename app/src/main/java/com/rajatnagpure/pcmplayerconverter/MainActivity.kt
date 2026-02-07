package com.rajatnagpure.pcmplayerconverter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.rajatnagpure.pcmplayerconverter.ui.navigation.AppNavigation
import com.rajatnagpure.pcmplayerconverter.ui.theme.PCMPlayerConverterTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PCMPlayerConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    

                    // We need to modify AppNavigation to accept navController or move Intent handling there.
                    // But AppNavigation creates its own navController.
                    // Let's modify AppNavigation to accept navController or handle intent internally?
                    // Simpler: Just Copy AppNavigation content here or modify AppNavigation.
                    // For now, I will use a slightly modified AppNavigation that accepts a navController,
                    // OR I will just rely on the fact that for a fresh start, I can't easily push to the internal navController of AppNavigation from here.
                    // ACTUALLY, AppNavigation creates `rememberNavController`.
                    // I should pass it `startDestination` with arguments if intent exists?
                    // Better: Modify AppNavigation to take `intentUri`?
                    
                    // Let's pass the intent URI to AppNavigation if strictly needed, 
                    // but simpler is to handle it inside AppNavigation or pass the controller.
                    // I will change AppNavigation to take `navController` as parameter or just `startDestination`.
                    
                    // Let's stick to the current AppNavigation which instantiates its own controller.
                    // It makes external navigation hard.
                    // I'll update AppNavigation to accept `navController`.
                    
                    // Initial URI from Intent
                    var startDestination = "converter"
                    if (intent?.action == Intent.ACTION_SEND) {
                         val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                         val mimeType = intent.type
                         
                         uri?.let {
                             val encodedUri = URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString())
                             // Route logic: pcm -> converter, others -> generator
                             val isPcm = mimeType?.contains("pcm") == true || it.toString().lowercase().endsWith(".pcm")
                             startDestination = if (isPcm) "converter?uri=$encodedUri" else "generator?uri=$encodedUri"
                         }
                    }
                    
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}