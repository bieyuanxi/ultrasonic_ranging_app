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
import com.example.myapplication.sound.AudioRecordManager
import com.example.myapplication.sound.AudioTrackManager
import com.example.myapplication.sound.RecordingListener
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

    private lateinit var audioRecordManager: AudioRecordManager
    private lateinit var audioTrackManager: AudioTrackManager

    private var lineDataEntries = mutableStateListOf<Entry>()

    private val isPlayingState = mutableStateOf(false)
    private val isRecordingState = mutableStateOf(false)

    private val m = mutableStateOf(0)
    private val phi = mutableStateOf(0.0)


    private val oddDiscreteImpulseTrain =  discreteImpulseTrain(81, true)
    private val evenDiscreteImpulseTrain =  discreteImpulseTrain(81, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logDeviceInfo()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        audioTrackManager = AudioTrackManager()
        audioRecordManager = AudioRecordManager()

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
                            } else {
                                val audioData = genAudioData()
                                playSound(audioData)
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
                            } else {
                                val audioData = genEvenAudioData()
                                playSound(audioData)
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
                            } else {
                                val audioData = genOddAudioData()
                                playSound(audioData)
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
    }

    private fun playSound(audioData: FloatArray, loopCount: Int = -1) {
        isPlayingState.value = true
        audioTrackManager.playSound(audioData, loopCount)
    }

    private fun stopPlaying() {
        audioTrackManager.stopPlaying()
        isPlayingState.value = false
    }

    private fun startRecording(odd: List<Int>? = null, ) {
        isRecordingState.value = true
        audioRecordManager.startRecording(odd, listener = { cir ->
            val mag = magnitude(cir)
            val maxIndexedValue = mag.withIndex().maxByOrNull { it.value }
            if (maxIndexedValue != null) {
                m.value = maxIndexedValue.index
                phi.value = calculatePhaseShift(cir[maxIndexedValue.index])
            }

            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    lineDataEntries = lineDataEntries.apply {
                        clear()
                        addAll(mag.mapIndexed { index, d -> Entry(index.toFloat(), d.toFloat()) })
                    }
                }
            }
        })
    }

    private fun stopRecording() {
        audioRecordManager.stopRecording()
        isRecordingState.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaying()
        if (isRecordingState.value) {
            stopRecording()
        }
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

    private fun logDeviceInfo() {
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