package com.example.pcmplayerconverter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*
import java.lang.Integer.parseInt

class MainActivity : AppCompatActivity() {
    private var fragment: FileChooserFragment? = null
    private var encodingSpinner: Spinner? = null
    private var channelsSpinner: Spinner? = null
    private var samplingRateEditText: EditText? = null
    private var playButton: ImageButton? = null
    private var convertToMp3: Button? = null
    private var convertToWav: Button? = null
    private var filePath = ""
    private var samplingRate = 8000
    private var encoding = 0x10
    private var channel = 0x10
    private val conversionFunctions = ConversionFunctions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var fragmentManager: androidx.fragment.app.FragmentManager = this.supportFragmentManager
        this.fragment = fragmentManager.findFragmentById(R.id.fragment_fileChooser) as FileChooserFragment

        encodingSpinner = findViewById(R.id.encoding_spinner)
        channelsSpinner = findViewById(R.id.channels_spinner)
        samplingRateEditText = findViewById(R.id.sampling_rate_edit_text)
        playButton = findViewById(R.id.play_button)
        convertToMp3 = findViewById(R.id.convert_to_mp3_button)
        convertToWav = findViewById(R.id.convert_to_wav_button)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val audioFile: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (audioFile != null) {
                    filePath = audioFile.path.toString().replace("/external_dir","")
                    filePath = "/storage/emulated/0$filePath"
                    Log.d("Rajat", filePath)
                    fragment!!.path = filePath
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val audioFileList: ArrayList<Uri> = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!
                for (uri in audioFileList) {
                    Toast.makeText(this, "Please select single file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val encodings: MutableList<String> = ArrayList()
        encodings.add("PCM 8-bit")
        encodings.add("PCM 16-bit")
        val channels: MutableList<String> = ArrayList()
        channels.add("Mono (1 Channel)")
        channels.add("Stereo (2 Channels)")

        val help = findViewById<CardView>(R.id.help)
        help.setOnClickListener{
            val notLoadingIntent = Intent(this, NeedHelp::class.java)
            startActivity(notLoadingIntent);
        }

        val encodingAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item, encodings
        )
        encodingAdapter.setDropDownViewResource(android.R.layout.select_dialog_item)
        encodingSpinner?.adapter = encodingAdapter

        val channelAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item, channels
        )
        channelAdapter.setDropDownViewResource(android.R.layout.select_dialog_item)
        channelsSpinner?.adapter = channelAdapter


        playButton?.setOnClickListener{
            if(!updateValues()) return@setOnClickListener
            if(filePath.isEmpty()){
                Toast.makeText(this, "No File Selected!!!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (filePath.substring(filePath.lastIndexOf('.') + 1) != "pcm"){
                Toast.makeText(this, "Please Select a PCM file!!!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                if(!convert(WAV)){
                    return@setOnClickListener
                }
                val intent = Intent(this, MusicPlayer::class.java)
                intent.putExtra("uri", filePath.removeSuffix("pcm") + "wav")
                startActivity(intent)
            }
        }

        convertToWav?.setOnClickListener{
            if(!updateValues()) return@setOnClickListener
            if(filePath.isEmpty()){
                Toast.makeText(this, "No File Selected!!!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (filePath.substring(filePath.lastIndexOf('.') + 1) != "pcm"){
                Toast.makeText(this, "Please Select a PCM file!!!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                if(!convert(WAV)){
                    return@setOnClickListener
                }
                Toast.makeText(
                    this,
                    "Wave File Stored at: " + filePath.removeSuffix("pcm") + "wav",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        convertToMp3?.setOnClickListener{
            Toast.makeText(this, "Coming Soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convert(format: Int): Boolean{
        val fileIn = File(filePath)
        if(!fileIn.exists()) {
            Toast.makeText(this, "File Does Not Exist! Check File Path!!!", Toast.LENGTH_LONG).show()
            return false
        }
        var fileOut:File? = null
        try {
            if(format == MP3){
                fileOut = File(filePath.removeSuffix("pcm") + "mp3")
                conversionFunctions.rawToWave(
                    samplingRate, encoding.toShort(),
                    channel.toShort(), fileIn, fileOut
                )
            }else{
                fileOut = File(filePath.removeSuffix("pcm") + "wav")
                conversionFunctions.rawToWave(
                    samplingRate,
                    encoding.toShort(), channel.toShort(), File(filePath), fileOut
                )
            }
        }catch (e: Exception){
            Toast.makeText(this, "Error! in conversion function", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun updateValues(): Boolean {
        filePath = fragment!!.path
        encoding = when(encodingSpinner?.selectedItem as String){
            "PCM 8-bit" -> 8
            "PCM 16-bit" -> 16
            else -> 8
        }
        channel = when(channelsSpinner?.selectedItem as String){
            "Mono (1 Channel)" -> 1
            "Stereo (2 Channels)" -> 2
            else-> AudioFormat.CHANNEL_IN_MONO
        }
        try {
            samplingRate = parseInt(samplingRateEditText!!.text.toString())
        }catch (e: NumberFormatException){
            Toast.makeText(this, "Sampling Rate is Wrong!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkPermission(permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(permission),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }
    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
        private const val MP3 = 2
        private const val WAV = 1
    }
}