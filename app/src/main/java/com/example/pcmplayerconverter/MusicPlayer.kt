package com.example.pcmplayerconverter

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

class MusicPlayer : AppCompatActivity() {
    private var playPauseButton: ImageButton? = null
    private var forward5Sec: ImageButton? = null
    private var rewind5Sec: ImageButton? = null
    private val iv: ImageView? = null
    private var mediaPlayer: MediaPlayer? = null

    private var startTime = 0
    private var finalTime = 0
    private var play = true
    private var oneTimeOnly = 0

    private var myHandler: Handler = Handler()
    private val forwardTime = 5000
    private val backwardTime = 5000
    private var seekbar: SeekBar? = null
    private var songText: TextView? = null
    private var totalTime: TextView? = null
    private var currentTime: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        playPauseButton = findViewById(R.id.play_pause_button)
        forward5Sec = findViewById(R.id.forward_5_sec_button)
        rewind5Sec = findViewById(R.id.rewind_5_sec_button)
        seekbar = findViewById(R.id.seekBar)
        songText = findViewById(R.id.song_text)
        currentTime = findViewById(R.id.current_time_text)
        totalTime = findViewById(R.id.total_time_text)

//        val uri = "/storage/emulated/0/buffer/t1.mp3"
        val myIntent = intent // gets the previously created intent
        var uri = myIntent.getStringExtra("uri")
        mediaPlayer = MediaPlayer.create(this, android.net.Uri.parse(uri))
        mediaPlayer?.start()
        playPauseButton?.setBackgroundResource(R.drawable.ic_baseline_pause_24)

        myHandler.postDelayed(updateSongTime, 100)
        finalTime = (mediaPlayer?.duration!!)
        startTime = (mediaPlayer?.currentPosition!!)
        seekbar?.isClickable = false
        if (oneTimeOnly == 0) {
            seekbar?.max = finalTime
            oneTimeOnly = 1
        }
        seekbar?.progress = startTime

        totalTime?.text = (kotlin.String.format(
            "%d:%d",
            TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()))
        ))
        currentTime?.text = (kotlin.String.format(
            "%d:%d",
            TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()))
        ))
        uri = uri?.removeSuffix("wav")+"pcm"
        songText?.text = uri?.substring(uri?.lastIndexOf('/') + 1)

        playPauseButton?.setOnClickListener{
            if(!play){
                mediaPlayer?.start()
                playPauseButton?.setBackgroundResource(R.drawable.ic_baseline_pause_24)
                play = !play
            }else{
                mediaPlayer?.pause()
                playPauseButton?.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
                play = !play
            }
        }

        forward5Sec?.setOnClickListener{
            mediaPlayer?.pause()
            startTime = mediaPlayer?.currentPosition!!

            if((startTime+forwardTime)<=finalTime){
                startTime += forwardTime;
                mediaPlayer?.seekTo(startTime)
            }
            seekbar?.progress = startTime
            mediaPlayer?.start()
        }

        rewind5Sec?.setOnClickListener{
            mediaPlayer?.pause()
            startTime = mediaPlayer?.currentPosition!!

            if((startTime-backwardTime)>0){
                startTime -= backwardTime;
                mediaPlayer?.seekTo(startTime)
            }
            seekbar?.progress = startTime
            mediaPlayer?.start()
        }
    }

    override fun onResume() {
        super.onResume()
        myHandler.postDelayed(updateSongTime, 100)
        startTime = (mediaPlayer?.currentPosition!!)
        seekbar?.progress = startTime
        mediaPlayer?.start()

        currentTime?.text = (kotlin.String.format(
            "%d:%d",
            TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()))
        ))
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.pause()
    }

    private val updateSongTime: Runnable = object : Runnable {
        override fun run() {
            if(mediaPlayer?.isPlaying == true) {
                startTime = (mediaPlayer!!.currentPosition)
                currentTime?.text = (String.format(
                    "%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()))
                ))
                seekbar?.progress = startTime
                myHandler.postDelayed(this, 100)
            }
        }
    }
}