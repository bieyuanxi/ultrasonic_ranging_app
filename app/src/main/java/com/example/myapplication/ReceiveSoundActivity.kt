package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ReceiveSoundActivity : ComponentActivity() {

    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isRecordingState = remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "声波接收示例")
                Spacer(modifier = Modifier.height(20.dp))
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
        }
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
            SAMPLE_RATE,
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
        if (isRecording) {
            stopRecording()
        }
    }
}