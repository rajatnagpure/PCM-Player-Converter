package com.rajatnagpure.pcmplayerconverter

import android.content.Intent
import android.util.Log
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        setTheme(R.style.Theme_PCMPlayerConverter)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PCMPlayerConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Observe the custom state properly
                    val event = _intentRouteEvent.value
                    AppNavigation(intentRouteEvent = event)
                }
            }
        }
        
        // Process initial intent after setContent so navigation components are ready
        processIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        try {
            val action = intent?.action
            Log.d(TAG, "Processing intent: action=$action")
            
            val intentUri = when (action) {
                Intent.ACTION_SEND -> {
                    // Modern Android often puts the URI in ClipData
                    intent.clipData?.let { clipData ->
                        if (clipData.itemCount > 0) {
                            clipData.getItemAt(0).uri
                        } else null
                    } ?: intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                Intent.ACTION_VIEW -> intent.data
                else -> null
            }

            intentUri?.let { uri ->
                Log.d(TAG, "Intent URI found: $uri")
                
                // Temporary debug toast for runtime feedback
                android.widget.Toast.makeText(this, "File received: ${uri.lastPathSegment}", android.widget.Toast.LENGTH_SHORT).show()
                
                // Use android.net.Uri.encode to produce an encoded parameter compatible with NavController
                val encodedUri = android.net.Uri.encode(uri.toString())
                
                val contentResolver = applicationContext.contentResolver
                val mimeType = contentResolver.getType(uri) ?: intent?.type
                Log.d(TAG, "MIME type: $mimeType")
                
                // Determine file type - prioritize file extension over MIME type
                val uriString = uri.toString().lowercase()
                val isPcm = uriString.endsWith(".pcm") || 
                            mimeType?.contains("pcm", ignoreCase = true) == true

                // Treat other audio types (wav, mp3, m4a, aac, etc.) as generator inputs
                val audioExtensions = listOf(".wav", ".mp3", ".m4a", ".aac", ".flac", ".ogg", ".wma")
                val isAudioExtension = audioExtensions.any { uriString.endsWith(it) }
                val isAudioMime = mimeType?.startsWith("audio", ignoreCase = true) == true
                val isAudio = !isPcm && (isAudioExtension || isAudioMime)

                val route = when {
                    isPcm -> "converter?uri=$encodedUri"
                    isAudio -> "generator?uri=$encodedUri"
                    else -> "generator?uri=$encodedUri" // default to generator for unknown files
                }

                android.util.Log.d(TAG, "Generated intent route: $route")
                _intentRouteEvent.value = IntentRouteEvent(route)
            } ?: run {
                Log.d(TAG, "No URI found in intent")
                if (action == Intent.ACTION_SEND || action == Intent.ACTION_VIEW) {
                    android.widget.Toast.makeText(this, "Could not reveal file in intent", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing intent", e)
            android.widget.Toast.makeText(this, "Error processing shared file", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}