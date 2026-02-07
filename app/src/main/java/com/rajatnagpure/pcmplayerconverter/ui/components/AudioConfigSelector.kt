package com.rajatnagpure.pcmplayerconverter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.model.PcmEncoding
import com.rajatnagpure.pcmplayerconverter.domain.model.SUPPORTED_SAMPLE_RATES

@Composable
fun AudioConfigSelector(
    config: AudioConfig,
    onConfigChange: (AudioConfig) -> Unit
) {
    val sampleRateOptions = SUPPORTED_SAMPLE_RATES.map { it.toString() } + "Custom..."
    var isCustomSampleRate by remember { 
        mutableStateOf(!SUPPORTED_SAMPLE_RATES.contains(config.sampleRate)) 
    }

    Column {
        // Sample Rate Dropdown
        DropdownSelector(
            label = "Sample Rate",
            options = sampleRateOptions,
            selectedOption = if (isCustomSampleRate) "Custom..." else config.sampleRate.toString(),
            onOptionSelected = { 
                if (it == "Custom...") {
                    isCustomSampleRate = true
                } else {
                    isCustomSampleRate = false
                    onConfigChange(config.copy(sampleRate = it.toInt()))
                }
            }
        )

        // Custom Sample Rate Input
        if (isCustomSampleRate) {
            OutlinedTextField(
                value = config.sampleRate.toString(),
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() }
                    if (filtered.isNotEmpty()) {
                        onConfigChange(config.copy(sampleRate = filtered.toInt()))
                    } else {
                        onConfigChange(config.copy(sampleRate = 0))
                    }
                },
                label = { Text("Enter Custom Rate (Hz)", color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue,
                    unfocusedBorderColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue.copy(alpha = 0.5f),
                    focusedLabelColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue,
                    unfocusedLabelColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue.copy(alpha = 0.7f)
                )
            )
        }

        // Channels Dropdown
        DropdownSelector(
             label = "Channels",
             options = listOf("Mono", "Stereo"),
             selectedOption = if (config.channels == 1) "Mono" else "Stereo",
             onOptionSelected = { onConfigChange(config.copy(channels = if (it == "Mono") 1 else 2)) }
        )

        // Encoding Dropdown
        DropdownSelector(
            label = "Encoding",
            options = PcmEncoding.values().map { it.description },
            selectedOption = config.encoding.description,
            onOptionSelected = { desc ->
                val encoding = PcmEncoding.values().find { it.description == desc } ?: PcmEncoding.BIT_16
                onConfigChange(config.copy(encoding = encoding))
            }
        )
        
        // Output Format Dropdown
        DropdownSelector(
            label = "Output Format",
            options = com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.values().map { it.description },
            selectedOption = config.outputFormat.description,
            onOptionSelected = { desc ->
                val format = com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.values().find { it.description == desc } ?: com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.WAV
                onConfigChange(config.copy(outputFormat = format))
            }
        )
        
        // VBR Toggle (Only for M4A)
        if (config.outputFormat == com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.M4A) {
             Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
             ) {
                 Text(text = "Variable Bitrate (VBR)", modifier = Modifier.weight(1f))
                 Switch(
                     checked = config.enableVbr,
                     onCheckedChange = { onConfigChange(config.copy(enableVbr = it)) }
                 )
             }
        }
    }
}

@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label, color = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue) },
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, "Dropdown", tint = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue,
                unfocusedBorderColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue.copy(alpha = 0.5f),
                focusedLabelColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue,
                unfocusedLabelColor = com.rajatnagpure.pcmplayerconverter.ui.theme.DarkBlue.copy(alpha = 0.7f)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
        // Invisible clickable surface
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
