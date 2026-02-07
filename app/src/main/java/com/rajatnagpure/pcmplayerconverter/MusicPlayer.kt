package com.rajatnagpure.pcmplayerconverter

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.rajatnagpure.pcmplayerconverter.ui.MusicPlayerScreen
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit

class MusicPlayer : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getStringExtra("uri")
        uri?.let {
            mediaPlayer = MediaPlayer.create(this, android.net.Uri.parse(it))
            mediaPlayer?.start()
        }

        setContent {
            var isPaused by remember { mutableStateOf(false) }
            var currentPosition by remember { mutableStateOf(0) }
            var duration by remember { mutableStateOf(mediaPlayer?.duration ?: 0) }

            LaunchedEffect(Unit) {
                while (true) {
                    if (mediaPlayer?.isPlaying == true) {
                        currentPosition = mediaPlayer?.currentPosition ?: 0
                    }
                    delay(100)
                }
            }

            mediaPlayer?.setOnCompletionListener {
                isPaused = true
                currentPosition = 0
                mediaPlayer?.seekTo(0)
            }

            val pcmUri = uri?.removeSuffix("wav") + "pcm"
            val songTitle = pcmUri.substring(pcmUri.lastIndexOf('/') + 1)

            MusicPlayerScreen(
                songTitle = songTitle,
                currentTime = formatTime(currentPosition.toLong()),
                totalTime = formatTime(duration.toLong()),
                progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                isPaused = isPaused,
                onPlayPauseClick = {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        isPaused = true
                    } else {
                        mediaPlayer?.start()
                        isPaused = false
                    }
                },
                onRewindClick = {
                    val newPos = (mediaPlayer?.currentPosition ?: 0) - 5000
                    mediaPlayer?.seekTo(newPos.coerceAtLeast(0))
                    currentPosition = mediaPlayer?.currentPosition ?: 0
                },
                onForwardClick = {
                    val newPos = (mediaPlayer?.currentPosition ?: 0) + 5000
                    mediaPlayer?.seekTo(newPos.coerceAtMost(duration))
                    currentPosition = mediaPlayer?.currentPosition ?: 0
                },
                onSeekChange = { progress ->
                    val newPos = (progress * duration).toInt()
                    mediaPlayer?.seekTo(newPos)
                    currentPosition = newPos
                }
            )
        }
    }

    private fun formatTime(millis: Long): String {
        return String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}