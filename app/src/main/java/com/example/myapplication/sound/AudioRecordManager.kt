package com.example.myapplication.sound

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.myapplication.Complex
import com.example.myapplication.conjugation
import com.example.myapplication.demodulate
import com.example.myapplication.dft
import com.example.myapplication.generateZCSequence
import com.example.myapplication.shiftRight
import java.io.IOException

fun interface RecordingListener {
    fun onDataAvailable(cir: List<Complex>)
}

class AudioRecordManager {
    private val SAMPLE_RATE = 48000
    private val FRAME_LEN = 960

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null



    @SuppressLint("MissingPermission")
    fun startRecording(odd: List<Int>? = null, listener: RecordingListener) {
        val u = 1
        val q = 81
        val Nzc = 81
        val h_zc = Nzc / 2
        val zc = generateZCSequence(u, q, Nzc)
        val ZC = dft(zc)
        val ZC_hat = shiftRight(ZC, h_zc)
        val ZC_hat_prime = conjugation(ZC_hat)

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

                    listener.onDataAvailable(cir)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        recordingThread?.start()
    }

    fun stopRecording() {
        recordingThread?.interrupt()
        recordingThread?.join()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}