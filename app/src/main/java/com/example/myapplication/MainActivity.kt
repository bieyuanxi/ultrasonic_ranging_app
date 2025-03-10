package com.example.myapplication

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import java.io.IOException
import kotlin.math.sin

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val SAMPLE_RATE = 48000
    private val FRAME_LEN = 960

    // 发送声波相关变量
    private var audioTrack: AudioTrack? = null
    private var isPlayingState = mutableStateOf(false)
    private var playingThread: Thread? = null

    // 接收声波相关变量

    private var audioRecord: AudioRecord? = null
    private var isRecordingState = mutableStateOf(false)
    private var recordingThread: Thread? = null

    private var lineDataEntries = mutableStateListOf<Entry>()

    private val m = mutableStateOf(0)
    private val phi = mutableStateOf(0.0)


    private val oddDiscreteImpulseTrain =  discreteImpulseTrain(81, true)
    private val evenDiscreteImpulseTrain =  discreteImpulseTrain(81, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "声波发送和接收示例")
                Row {
                    Button(
                        onClick = {
                            if (isPlayingState.value) {
                                stopPlaying()
                                isPlayingState.value = false
                            } else {
                                val u = 1
                                val q = 81
                                val Nzc = 81
                                val h_zc = Nzc / 2
                                val zc = generateZCSequence(u, q, Nzc)
                                val ZC = dft(zc)
                                val ZC_hat = shiftRight(ZC, h_zc)

                                val N = FRAME_LEN     // frame length
                                val f_c = 19000 // carrier frequency
                                val f_s = this@MainActivity.SAMPLE_RATE // sampling frequency
                                val n_c = N * f_c / f_s
                                val x = modulate(N, f_c, f_s, ZC_hat).toMutableList()

                                val audioData = x.map { it.real.toFloat() }.toFloatArray()
                                playSound(audioData)
                                isPlayingState.value = true
                            }
                        }
                    ) {
                        Text(text = if (isPlayingState.value) "停止全带宽" else "发送全带宽")
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(
                        onClick = {
                            if (isPlayingState.value) {
                                stopPlaying()
                                isPlayingState.value = false
                            } else {
                                val u = 1
                                val q = 81
                                val Nzc = 81
                                val h_zc = Nzc / 2
                                val zc = generateZCSequence(u, q, Nzc)
                                val ZC = dft(zc)
                                val ZC_hat = shiftRight(ZC, h_zc)

                                val N = FRAME_LEN     // frame length
                                val f_c = 19000 // carrier frequency
                                val f_s = this@MainActivity.SAMPLE_RATE // sampling frequency
                                val n_c = N * f_c / f_s
                                val odd = false
                                val x = modulate(ZC_hat, N, f_c, f_s, discreteImpulseTrain(Nzc, odd)).toMutableList()

                                val audioData = x.map { it.real.toFloat() }.toFloatArray()
                                playSound(audioData)
                                isPlayingState.value = true
                            }
                        }
                    ) {
                        Text(text = if (isPlayingState.value) "停止even" else "发送even")
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(
                        onClick = {
                            if (isPlayingState.value) {
                                stopPlaying()
                                isPlayingState.value = false
                            } else {
                                val u = 1
                                val q = 81
                                val Nzc = 81
                                val h_zc = Nzc / 2
                                val zc = generateZCSequence(u, q, Nzc)
                                val ZC = dft(zc)
                                val ZC_hat = shiftRight(ZC, h_zc)

                                val N = FRAME_LEN     // frame length
                                val f_c = 19000 // carrier frequency
                                val f_s = this@MainActivity.SAMPLE_RATE // sampling frequency
                                val n_c = N * f_c / f_s
                                val odd = true
                                val x = modulate(ZC_hat, N, f_c, f_s, discreteImpulseTrain(Nzc, odd)).toMutableList()

                                val audioData = x.map { it.real.toFloat() }.toFloatArray()
                                playSound(audioData)
                                isPlayingState.value = true
                            }
                        }
                    ) {
                        Text(text = if (isPlayingState.value) "停止odd" else "发送odd")
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Row {
                    Button(
                        onClick = {
                            if (isRecordingState.value) {
                                stopRecording()
                            } else {
                                startRecording()
                            }
                        }
                    ) {
                        Text(text = if (isRecordingState.value) "停止录制" else "开始录制")
                    }
                    Button(
                        onClick = {
                            if (isRecordingState.value) {
                                stopRecording()
                            } else {
                                startRecording(evenDiscreteImpulseTrain)
                            }
                        }
                    ) {
                        Text(text = if (isRecordingState.value) "停止录制even" else "开始录制even")
                    }
                    Button(
                        onClick = {
                            if (isRecordingState.value) {
                                stopRecording()
                            } else {
                                startRecording(oddDiscreteImpulseTrain)
                            }
                        }
                    ) {
                        Text(text = if (isRecordingState.value) "停止录制odd" else "开始录制odd")
                    }
                }
                Text(text = "m = ${m.value}, phi = ${"%.3f".format(phi.value)}")

                WIFIP2PScreen { message ->
                    val intent = Intent(this@MainActivity, WifiP2P::class.java)
                    intent.putExtra("message", message)
                    startActivity(intent)
                }
                Spacer(modifier = Modifier.width(20.dp))

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        val lineChart = LineChart(context)
                        // 初始化数据
                        val dataSet = LineDataSet(lineDataEntries, "数据")
                        dataSet.color = android.graphics.Color.BLUE
                        dataSet.valueTextColor = android.graphics.Color.RED

                        val lineData = LineData(dataSet)
                        lineChart.data = lineData
                        lineChart.invalidate()
                        lineChart
                    },
                    update = { lineChart ->
                        // 数据更新时，更新图表
                        val dataSet = lineChart.data.getDataSetByIndex(0) as LineDataSet
                        dataSet.values = lineDataEntries
                        lineChart.data.notifyDataChanged()
                        lineChart.notifyDataSetChanged()
                        lineChart.invalidate()
                    }
                )
            }


        }

        Log.d("FEATURE_WIFI_AWARE", "${packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)}")

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

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun playSound(audioData: FloatArray, loopCount: Int = -1) {
        // 配置 AudioTrack 参数
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
        val bufferSizeInBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)

        playingThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)    // performance mode
                .build()

            audioTrack?.setVolume(0.8f)
            // 写入音频数据
            // In static buffer mode, copies the data to the buffer starting at offset 0, and the write mode is ignored.
            // Note that the actual playback of this data might occur after this function returns.
            audioTrack?.write(audioData, 0, audioData.size, AudioTrack.WRITE_BLOCKING)

            // 设置重复播放, loopCount = 重复播放次数（例如loopCount=k，则播放1+k次）
            val result = audioTrack?.setLoopPoints(0, audioData.size, loopCount)
            assert(result != AudioTrack.ERROR_BAD_VALUE)
            // 开始播放
            Log.d("playSound", "before call play()")
            audioTrack?.play()   // time?
            Log.d("playSound", "after call play()")
            isPlayingState.value = true

            if(loopCount >= 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    val playTimeInMs = (loopCount + 1) * 1000 * audioData.size / SAMPLE_RATE
                    Log.d("playSound", "will stop playing after ${playTimeInMs}ms")
                    delay(playTimeInMs.toLong())
                    stopPlaying()
                }
            }
        }

        playingThread?.start()
    }

    private fun stopPlaying() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        isPlayingState.value = false
        playingThread?.join()
    }

    private fun startRecording(odd: List<Int>? = null) {
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

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        Log.d("bufferSize", "$bufferSize")
        Log.d("preheating", "")
        audioRecord?.startRecording()
        isRecordingState.value = true
        Log.d("preheating done", "")
        recordingThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                val buffer = FloatArray(FRAME_LEN)
                var read: Int
                while (!Thread.currentThread().isInterrupted) {
                    read = audioRecord?.read(buffer, 0, FRAME_LEN, AudioRecord.READ_BLOCKING)?: 0
//                    Log.d("buffer", buffer.toList().toString())
//                    Log.d("audioRecord", "read len: $read")
                    val y = buffer.map { Complex(it.toDouble(), 0.0) }
                    // FIXME: GC & memory
                    val cir: List<Complex> = if (odd != null) {
                        demodulate(y, ZC_hat_prime, FRAME_LEN, I = odd)
                    } else {
                        demodulate(y, ZC_hat_prime, FRAME_LEN)
                    }
//                    val cir = demodulate(y, ZC_hat_prime, FRAME_LEN, I = oddDiscreteImpulseTrain)
//                    Log.d("cir", "$cir")
//                    val mag = signedMagnitude(cir)
                    val mag = magnitude(cir)
//                    Log.d("mag", mag.toString())
//                    Log.d("max_in_mag", mag.withIndex().maxByOrNull { it.value }.toString())
                    val maxIndexedValue = mag.withIndex().maxByOrNull { it.value }
                    if (maxIndexedValue != null) {
                        m.value = maxIndexedValue.index
                        phi.value = calculatePhaseShift(cir[maxIndexedValue.index])
                    }
//                    Log.d("max_in_mag", "...")


                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main) {
                            lineDataEntries = lineDataEntries.apply {
                                clear()
                                addAll(mag.mapIndexed { index, d -> Entry(index.toFloat(), d.toFloat()) })
                            }
                        }
                    }
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
        recordingThread?.interrupt()
        recordingThread?.join()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        isRecordingState.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaying()
        if (isRecordingState.value) {
            stopRecording()
        }
    }
}

@Composable
fun WIFIP2PScreen(onButtonClick: (String) -> Unit) {
    Column(
    ) {
        Button(onClick = { onButtonClick("来自第一个 Activity 的消息") }) {
            Text(text = "跳转到WIFIP2PScreen")
        }
    }
}