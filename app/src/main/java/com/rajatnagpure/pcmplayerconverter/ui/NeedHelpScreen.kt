package com.rajatnagpure.pcmplayerconverter.ui
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatnagpure.pcmplayerconverter.R
import com.rajatnagpure.pcmplayerconverter.ui.components.ExpandableNeuCard
import com.rajatnagpure.pcmplayerconverter.ui.components.NeuCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedHelpScreen(onBackClick: () -> Unit) {
    BackHandler(onBack = onBackClick)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PCM Knowledge Base") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Embedded Educational Diagram
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.pcm_sampling_diagram),
                    contentDescription = "Analog to Digital PCM Sampling Diagram",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Section 1: What is PCM
            ExpandableNeuCard(
                title = "WHAT IS PCM AUDIO?",
                initiallyExpanded = true
            ) {
                Text(
                    text = "PCM (Pulse-Code Modulation) is the standard method used to digitally represent analog signals. It is the standard form of digital audio in computers, CDs, digital telephony, and other digital audio applications.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Unlike WAV or MP3 files, raw PCM data does not contain a 'header'. A header is metadata that tells the audio player the sample rate, bit depth, and channels. Because PCM lacks this, you must manually specify these configurations to decode and play the file properly.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Section 2: Configurations
            ExpandableNeuCard(
                title = "UNDERSTANDING CONFIGS"
            ) {
                ConfigItem(
                    title = "Sample Rate (Hz)",
                    description = "How many times per second the audio was measured. Higher rates (e.g. 44100Hz for CDs) capture higher frequencies. Telephone audio is often 8000Hz."
                )
                ConfigItem(
                    title = "Bit Depth",
                    description = "The precision of each sample. 8-bit has background hiss, 16-bit is CD quality, 32-bit float is used in professional audio processing."
                )
                ConfigItem(
                    title = "Channels",
                    description = "Mono (1 channel) for voice recordings or single mic inputs. Stereo (2 channels) for left/right spatial audio."
                )
                ConfigItem(
                    title = "Endianness",
                    description = "The byte order in memory. Most modern systems use Little Endian. If audio sounds like pure static, try switching to Big Endian."
                )
            }

            // Section 3: Use Cases
            ExpandableNeuCard(
                title = "COMMON USE CASES"
            ) {
                Text(
                    text = "• Telecommunications & VoIP (often 8kHz u-law or raw PCM)\n" +
                           "• IoT & Embedded Systems (microcontrollers dumping raw sensor data)\n" +
                           "• Game Development (raw audio asset pipelines)\n" +
                           "• Data Recovery (extracting raw audio from corrupted WAV headers)",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Section 4: Troubleshooting
            ExpandableNeuCard(
                title = "TROUBLESHOOTING"
            ) {
                TroubleItem(
                    issue = "Sounds like extremely loud static/white noise?",
                    solution = "The Bit Depth or Endianness is wrong. Try switching from 16-bit to 8-bit, or Little Endian to Big Endian."
                )
                TroubleItem(
                    issue = "Sounds like a fast chipmunk?",
                    solution = "The selected Sample Rate is too high. The player is playing the samples faster than they were recorded. Lower it (e.g., from 44100 to 16000)."
                )
                TroubleItem(
                    issue = "Sounds very slow, deep, and demonic?",
                    solution = "The selected Sample Rate is too low. The player is playing the samples too slowly. Raise it (e.g., from 8000 to 44100)."
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ConfigItem(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TroubleItem(issue: String, solution: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp).padding(end = 4.dp)
            )
            Text(
                text = "Issue: $issue",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "Fix: $solution",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp)
        )
    }
}
