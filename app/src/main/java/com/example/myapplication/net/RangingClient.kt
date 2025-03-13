package com.example.wifidirect.net

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class RangingClient {
    private var clientJob: Job? = null
    private lateinit var socket: Socket

    // 启动客户端连接
    fun startClient(host: String, port: Int = 8888,  listener: ConnectionAcceptedListener? = null) {
        if (clientJob?.isActive == true) {
            return
        }
        clientJob = startClientJob(host, port, listener)
    }

    fun cancelClient() {
        clientJob?.cancel()
        socket.close()
        clientJob = null
    }

    private fun startClientJob(host: String, port: Int,  listener: ConnectionAcceptedListener? = null) = CoroutineScope(Dispatchers.IO).launch {
        Log.d("Client", "Starting client connection...")
        try {
            socket = Socket(host, port)
            Log.d("Client", "Connected to server: ${socket.inetAddress}")
//            handleConnection(socket)
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
            listener?.onConnectionAccepted(reader, writer)
            socket.close()
        } catch (e: IOException) {
            Log.e("Client", "Error: ${e.message}")
            e.printStackTrace()
        }
        Log.d("startClientJob", "Client job done.")
    }



    private suspend fun handleConnection(socket: Socket) = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))

        // 并发执行读写操作
        val readJob = launch {
            read(reader)
        }
        val writeJob = launch {
            // TODO: 根据业务修改write函数
            while (coroutineContext.isActive) {
                pingServer(writer)
            }

        }

        // 等待读写操作完成
        joinAll(readJob, writeJob)
        Log.d("Client", "disconnected: ${socket.inetAddress}")
    }
}

private suspend fun read(reader: BufferedReader) = withContext(Dispatchers.IO) {
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        Log.d("ClientReader", "Received from server: $line")
    }
}

private suspend fun write(writer: BufferedWriter, msg: String) = withContext(Dispatchers.IO) {
    writer.write(msg)
    writer.newLine()
    writer.flush()
    Log.d("ClientWriter", "Sent message to server")
}

private suspend fun pingServer(writer: BufferedWriter) = withContext(Dispatchers.IO) {
    while (coroutineContext.isActive) {
        write(writer, "ping")
        delay(1000)
    }
}