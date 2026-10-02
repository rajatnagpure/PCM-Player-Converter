package com.rajatnagpure.pcmplayerconverter.ui.screens
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

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
import androidx.navigation.NavBackStackEntry
import com.rajatnagpure.pcmplayerconverter.ui.components.AppButton
import com.rajatnagpure.pcmplayerconverter.ui.components.NeuCard
import com.rajatnagpure.pcmplayerconverter.ui.components.AudioConfigSelector
import com.rajatnagpure.pcmplayerconverter.ui.viewmodel.ConverterViewModel
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientStart
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientEnd
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ConverterScreen(
    backStackEntry: NavBackStackEntry,
    viewModel: ConverterViewModel = hiltViewModel(backStackEntry)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // React to nav argument once - ensure we only call when real uri present
    LaunchedEffect(Unit) {
        val uriFlow = backStackEntry.savedStateHandle.getStateFlow("uri", "{uri}")
        uriFlow.collectLatest { uriArg ->
            if (!uriArg.isNullOrBlank() && uriArg != "{uri}" && uriArg != "null") {
                try {
                    val parsed = Uri.parse(uriArg)
                    android.util.Log.d("ConverterScreen", "SavedStateHandle uri: $uriArg -> $parsed")
                    viewModel.onFileSelected(parsed)
                } catch (e: Exception) {
                    android.util.Log.e("ConverterScreen", "Error parsing savedStateHandle uri", e)
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.onFileSelected(it) } }
    )

    // Launcher for saving the file (Scoped Storage)
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(uiState.audioConfig.outputFormat.mimeType)
    ) { uri ->
        if (uri != null) {
            viewModel.saveFileToUri(uri)
        } else {
            viewModel.cancelSave()
        }
    }

    if (uiState.showSaveDialog) {
        // Trigger system picker instead of custom dialog
        // Side effect to launch the picker when the state flag is set
        LaunchedEffect(Unit) {
            saveLauncher.launch(uiState.suggestedFileName)
        }
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
        NeuCard {
            Text(
                text = "Input File",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
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

        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        
        Text(
            text = "Output Configuration",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
        )

        NeuCard {
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
                    color = MaterialTheme.colorScheme.primary,
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
                AppButton(
                    text = if (uiState.isPlaying) "Stop" else "Play Input",
                    icon = if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    onClick = { viewModel.togglePlay() },
                    enabled = !uiState.isConverting && uiState.selectedFile != null,
                    modifier = Modifier.weight(1f)
                )

                AppButton(
                    text = "Convert & Save",
                    icon = Icons.Default.Transform,
                    onClick = { viewModel.requestSaveFileName() },
                    enabled = !uiState.isConverting && uiState.selectedFile != null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
