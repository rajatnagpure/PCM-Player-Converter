package com.rajatnagpure.pcmplayerconverter.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.*
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
import com.rajatnagpure.pcmplayerconverter.ui.components.AppButton
import com.rajatnagpure.pcmplayerconverter.ui.components.AppCard
import com.rajatnagpure.pcmplayerconverter.ui.components.AudioConfigSelector
import com.rajatnagpure.pcmplayerconverter.ui.viewmodel.ConverterViewModel
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientStart
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientEnd

@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.onFileSelected(it) } }
    )

    if (uiState.showSaveDialog) {
        SaveFileDialog(
            suggestedName = uiState.suggestedFileName,
            onDismiss = { viewModel.cancelSave() },
            onConfirm = { viewModel.convertToFormat(it) }
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
        // File Selection Section
        AppCard {
            Text(
                text = "Input File",
                style = MaterialTheme.typography.labelLarge,
                color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = GradientEnd,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.selectedFile?.name ?: "No file selected",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                AppButton(
                    text = "Select",
                    icon = Icons.Default.FileOpen,
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.wrapContentSize()
                )
            }
        }

        Divider(color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue.copy(alpha = 0.1f))
        
        Text(
            text = "Output Configuration",
            style = MaterialTheme.typography.titleMedium,
            color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue,
            modifier = Modifier.align(Alignment.Start)
        )

        AppCard(reverseGradient = true) {
            AudioConfigSelector(
                config = uiState.audioConfig,
                onConfigChange = { viewModel.updateConfig(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.isConverting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = GradientEnd
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { viewModel.togglePlay() },
                    enabled = !uiState.isConverting && uiState.selectedFile != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uiState.isPlaying) "Stop" else "Play Input")
                }

                AppButton(
                    text = "Convert",
                    icon = Icons.Default.Transform,
                    onClick = { viewModel.requestSaveFileName() },
                    enabled = !uiState.isConverting && uiState.selectedFile != null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
