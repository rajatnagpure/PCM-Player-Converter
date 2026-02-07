package com.rajatnagpure.pcmplayerconverter.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Transform
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
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import com.rajatnagpure.pcmplayerconverter.ui.components.AppButton
import com.rajatnagpure.pcmplayerconverter.ui.components.AppCard
import com.rajatnagpure.pcmplayerconverter.ui.components.AppDialog
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
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }

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
            suggestedName = uiState.suggestedFileName,
            onDismiss = { viewModel.cancelSave() },
            onConfirm = { fileName -> viewModel.saveFile(fileName) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Layout: Select Audio & Record Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppButton(
                text = "Select Audio",
                icon = Icons.Default.AudioFile,
                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.weight(1f)
            )
            
            AppButton(
                text = if (uiState.isRecording) "Stop" else "Record",
                icon = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
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
        
        Text(
            "Recording Configuration", 
            style = MaterialTheme.typography.titleMedium, 
            color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue,
            modifier = Modifier.align(Alignment.Start)
        )
        
        AppCard {
            AudioConfigSelector(
                config = uiState.audioConfig,
                onConfigChange = { viewModel.updateConfig(it) }
            )
        }

        // Consolidated Area: Status, Waveform OR File info
        AppCard(reverseGradient = true) {
                if (uiState.isRecording) {
                    StringWaveformView(amplitude = uiState.currentAmplitude)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Recording...", color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue, style = MaterialTheme.typography.titleMedium)
                } else if (uiState.isConverting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = GradientEnd
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Converting to PCM...", color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue)
                } else if (uiState.selectedFileToConvert != null && !uiState.isRecording) {
                    // File details when selected
                    val file = uiState.selectedFileToConvert!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = GradientEnd)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("File to Convert", style = MaterialTheme.typography.titleMedium, color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Selected: ${file.name}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    AppButton(
                        text = "Convert to PCM",
                        icon = Icons.Default.Transform,
                        onClick = { viewModel.requestPcmSaveFileName() },
                        enabled = !uiState.isConverting,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Idle state
                    uiState.statusMessage?.let {
                        Text(text = it, color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue, textAlign = TextAlign.Center)
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
}

@Composable
fun StringWaveformView(amplitude: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val path = androidx.compose.ui.graphics.Path()
        
        val points = 100
        val dx = width / points
        
        path.moveTo(0f, centerY)
        
        for (i in 0..points) {
            val x = i * dx
            // Sine wave formula: y = A * sin(k*x + phase)
            // A depends on amplitude, k is frequency
            val waveHeight = (amplitude * 0.8f + 0.1f) * centerY
            val y = centerY + waveHeight * Math.sin((i.toDouble() / points.toDouble() * 2.0 * Math.PI * 2.0) + phase.toDouble()).toFloat()
            path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = GradientEnd,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        // Add a second subtle wave for depth
        val path2 = androidx.compose.ui.graphics.Path()
        path2.moveTo(0f, centerY)
        for (i in 0..points) {
            val x = i * dx
            val waveHeight = (amplitude * 0.5f + 0.05f) * centerY
            val y = centerY + waveHeight * Math.sin((i.toDouble() / points.toDouble() * 2.0 * Math.PI * 3.0) - phase.toDouble()).toFloat()
            path2.lineTo(x, y)
        }
        drawPath(
            path = path2,
            color = GradientStart.copy(alpha = 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

// AppButton moved to common components

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveFileDialog(
    suggestedName: String,
    onDismiss: () -> Unit, 
    onConfirm: (String) -> Unit
) {
    var fileName by remember { mutableStateOf(suggestedName) }
    
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save File", color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue) },
        text = {
            Column {
                Text("Enter filename:", color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue,
                        unfocusedIndicatorColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(fileName) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GradientEnd)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue)
            }
        }
    )
}
