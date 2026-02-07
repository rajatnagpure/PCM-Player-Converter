package com.rajatnagpure.pcmplayerconverter.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import com.rajatnagpure.pcmplayerconverter.ui.components.AudioConfigSelector
import com.rajatnagpure.pcmplayerconverter.ui.viewmodel.GeneratorViewModel
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientStart
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientEnd
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.toggleRecording()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.onFileSelectedForConversion(it) } }
    )

    if (uiState.showSaveDialog) {
        SaveFileDialog(
            onDismiss = { viewModel.cancelSave() },
            onConfirm = { fileName -> viewModel.saveRecording(fileName) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Layout: Select Audio & Record Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientButton(
                text = "📁 Select Audio",
                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.weight(1f)
            )
            
            GradientButton(
                text = if (uiState.isRecording) "⏹️ Stop" else "🎙️ Record",
                onClick = {
                    if (uiState.isRecording) {
                        viewModel.toggleRecording()
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.weight(1f),
                overrideColor = if (uiState.isRecording) MaterialTheme.colorScheme.error else null
            )
        }
        
        // Status & Waveform Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, shape = MaterialTheme.shapes.medium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isRecording) {
                    WaveformView(amplitude = uiState.currentAmplitude)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Recording...", color = MaterialTheme.colorScheme.primary)
                } else if (uiState.isConverting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Converting to PCM...", color = MaterialTheme.colorScheme.primary)
                } else {
                    uiState.statusMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    }
                    uiState.errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                    if (uiState.statusMessage == null && uiState.errorMessage == null) {
                        Text(text = "Ready to record or convert", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        Divider()

        // Selected File Area
        uiState.selectedFileToConvert?.let { file ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = MaterialTheme.shapes.medium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) // Very light gradient shade
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("File to Convert", style = MaterialTheme.typography.titleMedium)
                    Text("Selected: ${file.name}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "🔄 Convert to PCM",
                        onClick = { viewModel.convertToPcm() },
                        enabled = !uiState.isConverting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Recording Configuration", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, shape = MaterialTheme.shapes.medium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                AudioConfigSelector(
                    config = uiState.audioConfig,
                    onConfigChange = { viewModel.updateConfig(it) }
                )
            }
        }
    }
}

@Composable
fun WaveformView(amplitude: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barCount = 40
        val barWidth = width / barCount
        val gradient = Brush.verticalGradient(listOf(GradientStart, GradientEnd))

        for (i in 0 until barCount) {
            // Random variation + amplitude
            val variation = (Math.random().toFloat() * 0.2f) + 0.1f
            val barHeight = (amplitude + variation).coerceIn(0.1f, 1f) * height * 0.8f
            
            drawRect(
                brush = gradient,
                topLeft = androidx.compose.ui.geometry.Offset(i * barWidth + 2, centerY - (barHeight / 2)),
                size = androidx.compose.ui.geometry.Size(barWidth - 4, barHeight),
                alpha = 0.8f
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overrideColor: Color? = null
) {
    val brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (overrideColor != null) Modifier.background(overrideColor)
                else if (enabled) Modifier.background(brush)
                else Modifier.background(Color.Gray.copy(alpha = 0.5f))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveFileDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var fileName by remember { mutableStateOf("recorded_audio_${System.currentTimeMillis()}") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Recording") },
        text = {
            Column {
                Text("Enter filename for the PCM file:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(fileName) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
