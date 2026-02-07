package com.rajatnagpure.pcmplayerconverter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatnagpure.pcmplayerconverter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityScreen(
    filePath: String,
    onBrowseClick: () -> Unit,
    samplingRate: String,
    onSamplingRateChange: (String) -> Unit,
    encoding: String,
    onEncodingChange: (String) -> Unit,
    channel: String,
    onChannelChange: (String) -> Unit,
    onPlayClick: () -> Unit,
    onConvertWavClick: () -> Unit,
    onConvertMp3Click: () -> Unit,
    onHelpClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHelpClick() },
                color = Color.White,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = stringResource(id = R.string.file_doesn_t_exist_help),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp, top = 10.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PCM",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Player & Converter ",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 21.dp)
            )

            // File Chooser Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 21.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filePath,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected File Path") },
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onBrowseClick) {
                        Text("Browse")
                    }
                }
            }

            // Options Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 7.dp)
            ) {
                DropdownSelector(
                    label = "Encoding : ",
                    options = listOf("PCM 8-bit", "PCM 16-bit"),
                    selectedOption = encoding,
                    onOptionSelected = onEncodingChange
                )

                DropdownSelector(
                    label = "Channels : ",
                    options = listOf("Mono (1 Channel)", "Stereo (2 Channels)"),
                    selectedOption = channel,
                    onOptionSelected = onChannelChange
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sampling Rate : ", modifier = Modifier.width(120.dp))
                    OutlinedTextField(
                        value = samplingRate,
                        onValueChange = onSamplingRateChange,
                        modifier = Modifier.width(160.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Play Button
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(text = "Convert to:")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onConvertMp3Click,
                    modifier = Modifier.padding(end = 7.dp)
                ) {
                    Text("MP3")
                }
                Button(
                    onClick = onConvertWavClick,
                    modifier = Modifier.padding(start = 7.dp)
                ) {
                    Text("Wav")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.width(120.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width(200.dp)
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(text = selectionOption) },
                        onClick = {
                            onOptionSelected(selectionOption)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
