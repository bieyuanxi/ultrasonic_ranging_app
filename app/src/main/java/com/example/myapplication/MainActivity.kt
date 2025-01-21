package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    // 发送声波相关变量
    private val SAMPLE_RATE_SEND = 48000
    private val DURATION_SEND = 2
    private val AMPLITUDE_SEND = 1000  // 振幅
    private var FREQUENCY_SEND = 20000
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var playingThread: Thread? = null

    // 接收声波相关变量
    private val SAMPLE_RATE_RECEIVE = 48000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_RECEIVE, CHANNEL_CONFIG, AUDIO_FORMAT)
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val SLIDE_RATE = 250
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isPlaying = remember { mutableStateOf(false) }
            val isRecordingState = remember { mutableStateOf(false) }
            val inputValue = remember { mutableStateOf(FREQUENCY_SEND.toString()) }
            val sliderValue = remember { mutableFloatStateOf(inputValue.value.toFloat() / SLIDE_RATE) }

            val soundList = remember { mutableStateListOf<Int>() }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "声波发送和接收示例")
                Row {
                    Button(
                        onClick = {
                            if (isPlaying.value) {
                                stopPlaying()
                                isPlaying.value = false
                            } else {
                                val numSamples = SAMPLE_RATE_SEND * DURATION_SEND
                                val audioData = generateSound(inputValue.value.toInt())
                                soundList.forEach { value ->
                                    val samples = generateSound(value)
                                    samples.forEachIndexed { index, sh ->
                                        if(sh + audioData[index] > Short.MAX_VALUE) {
                                            Log.e("amplitude", "amplitude overflow!")
                                        }
                                        audioData[index] = (sh + audioData[index]).toShort()
                                    }

                                }

                                playSound(audioData, numSamples)
                                isPlaying.value = true
                            }
                        }
                    ) {
                        Text(text = if (isPlaying.value) "停止发送" else "开始发送")
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(
                        onClick = {
                            if (isRecordingState.value) {
                                stopRecording()
                                isRecordingState.value = false
                            } else {
                                startRecording()
                                isRecordingState.value = true
                            }
                        }
                    ) {
                        Text(text = if (isRecordingState.value) "停止录制" else "开始录制")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row{
                    Text(text = "sound1")
                    Slider(
                        modifier = Modifier.width(200.dp),
                        value = sliderValue.floatValue,
                        onValueChange = { newSliderValue ->
                            sliderValue.floatValue = newSliderValue
                            inputValue.value = (newSliderValue * SLIDE_RATE).toInt().toString()

                        },
                        valueRange = 0f..100f
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        modifier = Modifier.width(80.dp),
                        value = inputValue.value,
                        onValueChange = { newInput ->
                            inputValue.value = newInput
                            val parsedValue = newInput.toFloat() / SLIDE_RATE
                            if (parsedValue in 0f..100f) {
                                sliderValue.floatValue = parsedValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                soundList.forEachIndexed { index, value ->
                    Row{
                        Text(text = "sound1")
                        Slider(
                            modifier = Modifier.width(200.dp),
                            value = value.toFloat(),
                            onValueChange = { newSliderValue ->
                                soundList[index] = newSliderValue.toInt()
                            },
                            valueRange = 0f..21000f
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            modifier = Modifier.width(80.dp),
                            value = value.toString(),
                            onValueChange = { newInput ->
//                                inputValue.value = newInput
                                val parsedValue = if (newInput.isNotEmpty()) newInput.toInt() else 0
                                if (parsedValue in 0..21000) {
                                    soundList[index] = parsedValue
                                }

                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }}
                Button(
                    onClick = {
                        soundList.add(1000)
                    }
                ) {
                    Text(text = "Add one")
                }
            }
        }
    }

    private fun generateSound(frequencySend: Int): ShortArray {
        val numSamples = SAMPLE_RATE_SEND * DURATION_SEND
        val audioData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val angle = i * (2 * Math.PI * frequencySend / SAMPLE_RATE_SEND)
            audioData[i] = (AMPLITUDE_SEND * sin(angle)).toInt().toShort()
        }

        return audioData
    }

    private fun playSound(audioData: ShortArray, numSamples: Int) {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_SEND,
            AudioFormat.CHANNEL_OUT_MONO,      // 通道数配置，这里选择了单通道（FL）
            AudioFormat.ENCODING_PCM_16BIT
        )

        playingThread = Thread {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE_SEND,
                AudioFormat.CHANNEL_OUT_MONO,   // 通道数配置，这里选择了单通道（FL）
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            isPlaying = true
            audioTrack?.play()
            audioTrack?.write(audioData, 0, numSamples)
        }

        playingThread?.start()
    }

    private fun stopPlaying() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        isPlaying = false
        playingThread?.join()
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_RECEIVE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            val file = File(Environment.getExternalStorageDirectory().absolutePath + "/recording.pcm")
            try {
                val fos = FileOutputStream(file)
                val buffer = ShortArray(BUFFER_SIZE)
                var read: Int
                while (isRecording) {
                    read = audioRecord?.read(buffer, 0, buffer.size)?: 0
                    if (read > 0) {
                        val byteBuffer = ByteArray(read * 2)
                        for (i in 0 until read) {
                            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = (buffer[i].toInt() shr 8).toByte()
                        }
                        fos.write(byteBuffer)
                    }
                }
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        recordingThread?.start()
    }

    private fun stopRecording() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        isRecording = false
        recordingThread?.join()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaying()
        if (isRecording) {
            stopRecording()
        }
    }
}

@Composable
fun listof(){

}

fun MainScreen() {

}