package com.example.myapplication.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioTrackManager {
    private val SAMPLE_RATE = 48000
    private val FRAME_LEN = 960
    // 发送声波相关变量
    private var audioTrack: AudioTrack? = null
    private var playingThread: Thread? = null

    fun playSound(audioData: FloatArray, loopCount: Int = -1) {
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

            audioTrack?.setVolume(1.0f)
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

    fun stopPlaying() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
//        playingThread?.join()
        CoroutineScope(Dispatchers.IO).launch {
            playingThread?.join()
            Log.d("playSound", "play thread stopped.")
        }
    }
}