package com.example.myapplication

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.myapplication.sound.AudioRecordManager
import com.example.myapplication.sound.AudioTrackManager
import com.example.wifidirect.net.RangingClient
import com.example.wifidirect.net.RangingServer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter

class MainActivity : ComponentActivity() {
    private lateinit var audioRecordManager: AudioRecordManager
    private lateinit var audioTrackManager: AudioTrackManager

    private val isPlayingState = mutableStateOf(false)
    private val isRecordingState = mutableStateOf(false)
    private var lineDataEntries = mutableStateListOf<Entry>()
    private val m = mutableIntStateOf(0)
    private val phi = mutableDoubleStateOf(0.0)
    private val distance = mutableFloatStateOf(0.0f)

    private val oddDiscreteImpulseTrain = discreteImpulseTrain(81, true)
    private val evenDiscreteImpulseTrain = discreteImpulseTrain(81, false)

    private val hostAddress = mutableStateOf("")
    private val rangingClient by lazy {
        RangingClient()
    }

    private val serverState = mutableStateOf(false)
    private val rangingServer by lazy {
        RangingServer()
    }

    private val peerList = mutableStateListOf<WifiP2pDevice>()
    private var wifiDirectService: WifiDirectService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WifiDirectService.LocalBinder
            wifiDirectService = binder.getService()
            wifiDirectService?.setDirectActionListener(directActionListener)
            Toast.makeText(this@MainActivity, "wifiDirectService connected", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            wifiDirectService = null
            Toast.makeText(this@MainActivity, "wifiDirectService disconnected", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private val directActionListener: DirectActionListener = object : DirectActionListener {
        override fun onWifiP2pStateChanged(enabled: Boolean) {
            Log.d("onP2pStateChanged", "$enabled")
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            Log.d("onConnInfoAvailable", "$wifiP2pInfo")
            if (wifiP2pInfo.isGroupOwner) {
                if (!serverState.value) {
                    startServerRangingWork()
                }
                serverState.value = true
            } else {
                wifiP2pInfo.groupOwnerAddress.hostAddress?.let { hostAddress.value =  it }
            }
        }

        override fun onDisconnection() {
            if (serverState.value) {
                rangingServer.cancelServer()
                serverState.value = false
            }
            hostAddress.value = ""


            Log.d("onDisconnection", "")
        }

        override fun onThisDeviceChanged(device: WifiP2pDevice) {
            Log.d("onThisDeviceChanged", "$device")
        }

        override fun onPeersListChanged(devices: List<WifiP2pDevice>) {
            peerList.clear()
            peerList.addAll(devices)
        }

        override fun onChannelDisconnected() {
            Log.d("onChannelDisconn", "")
        }

    }

    // 注册权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        Log.d("allPermissionsGranted", "$allPermissionsGranted")
        if (allPermissionsGranted) {
//            permissions.values.
        } else {
            Toast.makeText(this@MainActivity, "权限未获取", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    // >= android 13
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logDeviceInfo()

        checkAndRequestPermissions()

        audioTrackManager = AudioTrackManager()
        audioRecordManager = AudioRecordManager()

        val intent = Intent(this, WifiDirectService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "声波发送和接收示例")
//                Row {
//                    Button(
//                        onClick = {
//                            if (isPlayingState.value) {
//                                stopPlaying()
//                            } else {
//                                val audioData = genAudioData()
//                                playSound(audioData)
//                            }
//                        }
//                    ) {
//                        Text(text = if (isPlayingState.value) "停止全带宽" else "发送全带宽")
//                    }
//                    Spacer(modifier = Modifier.width(20.dp))
//                    Button(
//                        onClick = {
//                            if (isPlayingState.value) {
//                                stopPlaying()
//                            } else {
//                                val audioData = genEvenAudioData()
//                                playSound(audioData)
//                            }
//                        }
//                    ) {
//                        Text(text = if (isPlayingState.value) "停止even" else "发送even")
//                    }
//                    Spacer(modifier = Modifier.width(20.dp))
//                    Button(
//                        onClick = {
//                            if (isPlayingState.value) {
//                                stopPlaying()
//                            } else {
//                                val audioData = genOddAudioData()
//                                playSound(audioData)
//                            }
//                        }
//                    ) {
//                        Text(text = if (isPlayingState.value) "停止odd" else "发送odd")
//                    }
//                }
//                Spacer(modifier = Modifier.width(20.dp))
//                Row {
//                    Button(
//                        onClick = {
//                            if (isRecordingState.value) {
//                                stopRecording()
//                            } else {
//                                startRecording()
//                            }
//                        }
//                    ) {
//                        Text(text = if (isRecordingState.value) "停止录制" else "开始录制")
//                    }
//                    Button(
//                        onClick = {
//                            if (isRecordingState.value) {
//                                stopRecording()
//                            } else {
//                                startRecording(evenDiscreteImpulseTrain)
//                            }
//                        }
//                    ) {
//                        Text(text = if (isRecordingState.value) "停止录制even" else "开始录制even")
//                    }
//                    Button(
//                        onClick = {
//                            if (isRecordingState.value) {
//                                stopRecording()
//                            } else {
//                                startRecording(oddDiscreteImpulseTrain)
//                            }
//                        }
//                    ) {
//                        Text(text = if (isRecordingState.value) "停止录制odd" else "开始录制odd")
//                    }
//                }
                Text(
                    text = "m = ${m.intValue}, phi = ${"%.3f".format(phi.doubleValue)}, distance = ${
                        "%.3f".format(
                            distance.floatValue
                        )
                    }m"
                )
                Row {
                    Button(
                        onClick = {
                            wifiDirectService?.createGroup();
                        }
                    ) {
                        Text(text = "createGroup")
                    }
                    Button(
                        onClick = {
                            wifiDirectService?.removeGroup();
                        }
                    ) {
                        Text(text = "removeGroup")
                    }
                }
                Row {
                    Button(
                        onClick = {
                            wifiDirectService?.discoverPeers();
                        }
                    ) {
                        Text(text = "discoverPeers")
                    }
                    Button(
                        onClick = {
                            startClientRangingWork(hostAddress.value)
                        },
                        enabled = (hostAddress.value != "")
                    ) {
                        Text(text = "Start Ranging")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .height(100.dp)
                        .background(Color.LightGray)
                ) {
                    items(peerList) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(1.dp) // 外边距
                                .clickable { },
                            onClick = {
                                wifiDirectService?.connectToDevice(device);
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(15.dp) // 内边距
                            ) {
                                Text(text = "${device.deviceName}")
                            }
                        }
                    }
                }

//
//                WIFIP2PScreen { message ->
//                    val intent = Intent(this@MainActivity, WifiP2P::class.java)
//                    intent.putExtra("message", message)
//                    startActivity(intent)
//                }
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

    private fun startRecording(odd: List<Int>? = null) {
        isRecordingState.value = true
        audioRecordManager.startRecording(odd, listener = { cir ->
            val mag = magnitude(cir)
            val maxIndexedValue = mag.withIndex().maxByOrNull { it.value }
            if (maxIndexedValue != null) {
                m.intValue = maxIndexedValue.index
                phi.doubleValue = calculatePhaseShift(cir[maxIndexedValue.index])
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
        unbindService(serviceConnection)
    }

    private fun startServerRangingWork() {
        rangingServer.startServer() { reader: BufferedReader, writer: BufferedWriter ->
            val mIndex = intArrayOf(0, 0, 0)    // m_aa, m_ba, garbage
            var i = 0
            writer.write("startRecording\n")
            writer.flush()
            audioRecordManager.startRecording(listener = { cir ->
                val mag = magnitude(cir)
                val maxIndexedValue = mag.withIndex().maxByOrNull { it.value }
                if (maxIndexedValue != null) {
                    m.intValue = maxIndexedValue.index
                    phi.doubleValue = calculatePhaseShift(cir[maxIndexedValue.index])
                    mIndex[i] = maxIndexedValue.index
                }

                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        lineDataEntries = lineDataEntries.apply {
                            clear()
                            addAll(mag.mapIndexed { index, d ->
                                Entry(
                                    index.toFloat(),
                                    d.toFloat()
                                )
                            })
                        }
                    }
                }
            })
            delay(1000)

            writer.write("startServerAudioTrack\n")
            writer.flush()
            val audioData = genAudioData()
            playSound(audioData)
            reader.readLine()   //ClientRangingDone
            i = 1
            stopPlaying()

            delay(1000)
            writer.write("startClientAudioTrack\n")
            writer.flush()

            delay(1000)
            i = 2
            stopRecording()
            writer.write("ServerRangingDone\n")
            writer.flush()

            val str = reader.readLine()


            val m_aa = mIndex[0]
            val m_ba = mIndex[1]
            val parts = str.split(", ")
            val m_ab = parts[0].toInt()
            val m_bb = parts[1].toInt()
            Log.d("Distance", "(${m_aa}, ${m_ab}, ${m_bb}, ${m_ba})")

            writer.write("${m_aa}, ${m_ab}, ${m_bb}, ${m_ba}\n")
            writer.flush()

            distance.floatValue = get_distance(m_aa, m_ab, m_bb, m_ba)
        }
    }

    private fun startClientRangingWork(host: String) {
        rangingClient.startClient(host) { reader: BufferedReader, writer: BufferedWriter ->
            val mIndex = intArrayOf(0, 0, 0)    // m_ab, m_bb, garbage
            var i = 0
            reader.readLine();  // startRecording
            audioRecordManager.startRecording(listener = { cir ->
                val mag = magnitude(cir)
                val maxIndexedValue = mag.withIndex().maxByOrNull { it.value }
                if (maxIndexedValue != null) {
                    m.intValue = maxIndexedValue.index
                    phi.doubleValue = calculatePhaseShift(cir[maxIndexedValue.index])
                    mIndex[i] = maxIndexedValue.index
                }

                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        lineDataEntries = lineDataEntries.apply {
                            clear()
                            addAll(mag.mapIndexed { index, d ->
                                Entry(
                                    index.toFloat(),
                                    d.toFloat()
                                )
                            })
                        }
                    }
                }
            })
            reader.readLine();  // startServerAudioTrack
            delay(1000)
            i = 1
            writer.write("ClientRangingDone\n")    //
            writer.flush()

            reader.readLine();  // startClientAudioTrack

            val audioData = genAudioData()
            playSound(audioData)

            reader.readLine() // ServerRangingDone
            i = 2
            stopPlaying()

            writer.write("${mIndex[0]}, ${mIndex[1]}\n")
            writer.flush()
//            val m = reader.ready()

            val str = reader.readLine();  // distance
            val parts = str.split(", ")
            val m_aa = parts[0].toInt()
            val m_ba = parts[1].toInt()
            val m_bb = parts[2].toInt()
            val m_ab = parts[3].toInt()
            Log.d("Distance", "(${m_aa}, ${m_ab}, ${m_bb}, ${m_ba})")

            distance.floatValue = get_distance(m_aa, m_ab, m_bb, m_ba)

            stopRecording()
        }
    }


    private fun logDeviceInfo() {
        Log.d(
            "FEATURE_WIFI_AWARE",
            "${packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)}"
        )

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val isSupportMicNearUltrasound =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND)
        val isSupportSpeakerNearUltrasound =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND)
        Log.d(
            "SupportCheck",
            "MicUltrasound: ${isSupportMicNearUltrasound}, SpeakerUltrasound: $isSupportSpeakerNearUltrasound"
        )

        val pOutputSampleRate =
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val pOutputFramesPerBuffer =
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        Log.d(
            "SupportCheck",
            "pOutputSampleRate: $pOutputSampleRate, pOutputFramesPerBuffer: $pOutputFramesPerBuffer"
        )

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