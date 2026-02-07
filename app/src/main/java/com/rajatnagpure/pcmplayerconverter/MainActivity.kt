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
    
    // Data class to ensure every intent triggers a LaunchedEffect, even if the route is same
    data class IntentRouteEvent(val route: String, val timestamp: Long = System.currentTimeMillis())
    
    private val _intentRouteEvent = androidx.compose.runtime.mutableStateOf<IntentRouteEvent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        processIntent(intent)
        
        setContent {
            PCMPlayerConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(intentRouteEvent = _intentRouteEvent.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        val intentUri = if (intent?.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        } else if (intent?.action == Intent.ACTION_VIEW) {
            intent.data
        } else {
            null
        }

        intentUri?.let { it ->
            val encodedUri = URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString())
            
            val contentResolver = applicationContext.contentResolver
            val type = contentResolver.getType(it) ?: intent?.type
            
            val isPcm = type?.contains("pcm") == true || 
                        it.toString().lowercase().contains(".pcm") ||
                        (type == "application/octet-stream")
            
            val route = if (isPcm) {
                "converter?uri=$encodedUri"
            } else {
                "generator?uri=$encodedUri"
            }
            _intentRouteEvent.value = IntentRouteEvent(route)
        }
    }
}