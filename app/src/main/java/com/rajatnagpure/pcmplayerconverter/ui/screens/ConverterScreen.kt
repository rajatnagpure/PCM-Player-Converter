package com.rajatnagpure.pcmplayerconverter.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.rajatnagpure.pcmplayerconverter.ui.components.AudioConfigSelector
import com.rajatnagpure.pcmplayerconverter.ui.viewmodel.ConverterViewModel
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientStart
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientEnd

@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.onFileSelected(it) } }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // File Selection Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, shape = MaterialTheme.shapes.medium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Input File",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.selectedFile?.name ?: "No file selected",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(GradientStart, GradientEnd)
                                )
                            )
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("📁 Select")
                        }
                    }
                }
            }
        }

        // Configuration Section (Always Visible)
        Divider()
        
        Text(
            text = "Output Configuration",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, shape = MaterialTheme.shapes.medium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                AudioConfigSelector(
                    config = uiState.audioConfig,
                    onConfigChange = { viewModel.updateConfig(it) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (uiState.conversionMessage != null) {
                Text(
                    text = uiState.conversionMessage ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { viewModel.togglePlay() },
                    enabled = !uiState.isConverting && uiState.selectedFile != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(if (uiState.isPlaying) "⏹️ Stop" else "▶️ Play Input")
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            )
                        )
                ) {
                    Button(
                        onClick = { viewModel.convertToWav() },
                        enabled = !uiState.isConverting && uiState.selectedFile != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        if (uiState.isConverting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp).height(16.dp).width(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }
                        Text("🔄 Convert", color = Color.White)
                    }
                }
            }
        }
    }
}
