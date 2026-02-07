package com.rajatnagpure.pcmplayerconverter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import com.rajatnagpure.pcmplayerconverter.ui.MainActivityScreen
import java.io.File

class MainActivity : ComponentActivity() {
    private val conversionFunctions = ConversionFunctions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var filePath by remember { mutableStateOf("") }
            var samplingRate by remember { mutableStateOf("8000") }
            var encoding by remember { mutableStateOf("PCM 8-bit") }
            var channel by remember { mutableStateOf("Mono (1 Channel)") }

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    try {
                        val path = FileUtils.getPath(this, it)
                        if (path != null) {
                            filePath = path
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error getting file path", e)
                        Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Handle incoming intent (Share)
            LaunchedEffect(intent) {
                if (intent?.action == Intent.ACTION_SEND) {
                    val audioFile: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    audioFile?.let {
                        var path = it.path.toString().replace("/external_dir", "")
                        path = "/storage/emulated/0$path"
                        filePath = path
                    }
                }
            }

            MainActivityScreen(
                filePath = filePath,
                onBrowseClick = {
                    if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        filePickerLauncher.launch("*/*")
                    }
                },
                samplingRate = samplingRate,
                onSamplingRateChange = { samplingRate = it },
                encoding = encoding,
                onEncodingChange = { encoding = it },
                channel = channel,
                onChannelChange = { channel = it },
                onPlayClick = {
                    if (validateAndConvert(filePath, encoding, channel, samplingRate)) {
                        val intent = Intent(this, MusicPlayer::class.java)
                        intent.putExtra("uri", filePath.removeSuffix("pcm") + "wav")
                        startActivity(intent)
                    }
                },
                onConvertWavClick = {
                    if (validateAndConvert(filePath, encoding, channel, samplingRate)) {
                        Toast.makeText(
                            this,
                            "Wave File Stored at: " + filePath.removeSuffix("pcm") + "wav",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onConvertMp3Click = {
                    Toast.makeText(this, "Coming Soon!", Toast.LENGTH_SHORT).show()
                },
                onHelpClick = {
                    val intent = Intent(this, NeedHelp::class.java)
                    startActivity(intent)
                }
            )
        }
    }

    private fun validateAndConvert(
        filePath: String,
        encodingStr: String,
        channelStr: String,
        samplingRateStr: String
    ): Boolean {
        if (filePath.isEmpty()) {
            Toast.makeText(this, "No File Selected!!!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!filePath.endsWith(".pcm")) {
            Toast.makeText(this, "Please Select a PCM file!!!", Toast.LENGTH_SHORT).show()
            return false
        }

        val samplingRateVal = try {
            samplingRateStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Sampling Rate is Wrong!", Toast.LENGTH_SHORT).show()
            return false
        }

        val encodingVal = if (encodingStr == "PCM 8-bit") 8 else 16
        val channelVal = if (channelStr == "Mono (1 Channel)") 1 else 2

        if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return convert(filePath, samplingRateVal, encodingVal, channelVal)
        }
        return false
    }

    private fun convert(filePath: String, samplingRate: Int, encoding: Int, channel: Int): Boolean {
        val fileIn = File(filePath)
        if (!fileIn.exists()) {
            Toast.makeText(
                this,
                "File Does Not Exist!: Please see Need Help Page by clicking below Text.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        try {
            val fileOut = File(filePath.removeSuffix("pcm") + "wav")
            conversionFunctions.rawToWave(
                samplingRate,
                encoding.toShort(),
                channel.toShort(),
                fileIn,
                fileOut
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Conversion error", e)
            Toast.makeText(this, "Error! in conversion function", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkPermission(permission: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    if (permission == Manifest.permission.READ_EXTERNAL_STORAGE) 1000 else 102
                )
                return false
            }
        }
        return true
    }
}