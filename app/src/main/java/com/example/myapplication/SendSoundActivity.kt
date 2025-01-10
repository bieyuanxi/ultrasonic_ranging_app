package com.example.myapplication

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.sin

class SendSoundActivity : ComponentActivity() {

    private val SAMPLE_RATE = 44100
    private val DURATION = 2
    private val AMPLITUDE = 10000
    private val FREQUENCY = 1000
    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isPlaying = remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "声波发送示例")
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (isPlaying.value) {
                            stopPlaying()
                            isPlaying.value = false
                        } else {
                            generateAndPlaySound()
                            isPlaying.value = true
                        }
                    }
                ) {
                    Text(text = if (isPlaying.value) "停止发送" else "开始发送")
                }
            }
        }
    }

    private fun generateAndPlaySound() {
        val numSamples = SAMPLE_RATE * DURATION
        val audioData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val angle = i * (2 * Math.PI * FREQUENCY / SAMPLE_RATE)
            audioData[i] = (AMPLITUDE * sin(angle)).toInt().toShort()
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        audioTrack?.play()
        audioTrack?.write(audioData, 0, numSamples)
    }

    private fun stopPlaying() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaying()
    }
}