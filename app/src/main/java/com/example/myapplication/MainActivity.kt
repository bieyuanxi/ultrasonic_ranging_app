package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
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
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
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

                Spacer(modifier = Modifier.width(20.dp))
                // ZC
                Row {
                    Button(
                        onClick = {
                            if (isPlaying.value) {
                                stopPlaying()
                                isPlaying.value = false
                            } else {
                                val u = 1
                                val q = 81
                                val Nzc = 81
                                val h_zc = Nzc / 2
                                val zc = generateZCSequence(u, q, Nzc)
                                val ZC = dft(zc)
                                val ZC_hat = shiftRight(ZC, h_zc)

                                val N = 960     // frame length
                                val f_c = 19000 // carrier frequency
                                val f_s = SAMPLE_RATE_SEND // sampling frequency
                                val n_c = N * f_c / f_s
                                val x = modulate(N, f_c, f_s, ZC_hat).toMutableList()

                                val audioData = x.map { it.real.toFloat() }.toFloatArray()
                                playSound2(audioData)
                                isPlaying.value = true
                            }
                        }
                    ) {
                        Text(text = if (isPlaying.value) "停止发送ZC" else "开始发送ZC")
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
        assert(bufferSize != AudioTrack.ERROR_BAD_VALUE && bufferSize != AudioTrack.ERROR)
        playingThread = Thread {
            audioTrack = AudioTrack.Builder()
//                .setAudioAttributes(
//                    AudioAttributes.Builder()
//                        .setUsage(AudioAttributes.USAGE_MEDIA)
//                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                        .build()
//                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE_SEND)
//                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
//            audioTrack = AudioTrack(
//                AudioManager.STREAM_MUSIC,
//                SAMPLE_RATE_SEND,
//                AudioFormat.CHANNEL_OUT_MONO,   // 通道数配置，这里选择了单通道（FL）
//                AudioFormat.ENCODING_PCM_16BIT,
//                bufferSize,
//                AudioTrack.MODE_STREAM
//            )
            isPlaying = true
            audioTrack?.play()
            audioTrack?.write(audioData, 0, numSamples)
        }

        playingThread?.start()
    }

    private fun playSound1(audioData: FloatArray) {
        val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_SEND,
            channelConfig,      // 通道数配置，这里选择了单通道（FL）
            audioFormat
        )
        assert(bufferSize != AudioTrack.ERROR_BAD_VALUE && bufferSize != AudioTrack.ERROR)
        playingThread = Thread {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE_SEND,
                channelConfig,   // 通道数配置，这里选择了单通道（FL）
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.setVolume(0.8f)
            isPlaying = true
            audioTrack?.play()
            while (isPlaying) {
                assert(audioData.size * Float.SIZE_BYTES <= bufferSize)
                val count = audioTrack?.write(audioData, 0, audioData.size, AudioTrack.WRITE_BLOCKING)
                assert(count == audioData.size)
                Log.d("play_sound", "write $count")
            }

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null

        }

        playingThread?.start()
    }

    private fun playSound2(audioData: FloatArray) {
        // 配置 AudioTrack 参数
        val sampleRateInHz = 48000
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
        val bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

        playingThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRateInHz)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)    // performance mode
                .build()

            audioTrack.setVolume(0.8f)
            // 写入音频数据
            audioTrack.write(audioData, 0, audioData.size, AudioTrack.WRITE_BLOCKING)

            // 设置重复播放
            val result = audioTrack.setLoopPoints(0, audioData.size, 2)
            assert(result != AudioRecord.ERROR_BAD_VALUE)
            // 开始播放
            Log.d("playSound2", "before call play()")
            audioTrack.play()   // time?
            Log.d("playSound2", "after call play()")
        }

        playingThread?.start()
    }

    private fun stopPlaying() {
//        audioTrack?.stop()
//        audioTrack?.release()
//        audioTrack = null
        isPlaying = false
        playingThread?.join()
    }

    private fun startRecording() {
        val u = 1
        val q = 81
        val Nzc = 81
        val h_zc = Nzc / 2
        val zc = generateZCSequence(u, q, Nzc)
        val ZC = dft(zc)
        val ZC_hat = shiftRight(ZC, h_zc)
        val ZC_hat_prime = conjugation(ZC_hat)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_RECEIVE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE_RECEIVE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val isSupportMicNearUltrasound =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND)
        val isSupportSpeakerNearUltrasound =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND)
        Log.d("SupportCheck", "MicUltrasound: ${isSupportMicNearUltrasound}, SpeakerUltrasound: $isSupportSpeakerNearUltrasound")

        val pOutputSampleRate =
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val  pOutputFramesPerBuffer =
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        Log.d("SupportCheck", "pOutputSampleRate: $pOutputSampleRate, pOutputFramesPerBuffer: $pOutputFramesPerBuffer")

        val hasLowLatencyFeature: Boolean =
            packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)

        val hasProFeature: Boolean =
            packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)

        Log.d("hasLowLatencyFeature", "$hasLowLatencyFeature")
        Log.d("hasProFeature", "$hasProFeature")


        Log.d("preheating", "")
        audioRecord?.startRecording()
        isRecording = true
        Log.d("preheating done", "")
        recordingThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                val buffer = FloatArray(960)
                var read: Int
                while (isRecording) {
                    read = audioRecord?.read(buffer, 0, 960, AudioRecord.READ_BLOCKING)?: 0
//                    Log.d("buffer", buffer.toList().toString())
//                    Log.d("audioRecord", "read len: $read")
                    val y = buffer.map { Complex(it.toDouble(), 0.0) }
                    // FIXME: GC & memory
                    val cir = demodulate(y, ZC_hat_prime, 960)
                    val mag = magnitude(cir)
//                    Log.d("mag", mag.toString())
                    Log.d("max_in_mag", mag.withIndex().maxByOrNull { it.value }.toString())
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        recordingThread?.start()
    }

    // 注册权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限已授予，执行相应操作
        } else {
            // 权限被拒绝，给出提示或处理逻辑
        }
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

