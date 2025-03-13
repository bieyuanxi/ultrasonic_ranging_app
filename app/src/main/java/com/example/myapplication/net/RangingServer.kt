package com.example.wifidirect.net

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

fun interface ConnectionAcceptedListener {
    suspend fun onConnectionAccepted(reader: BufferedReader, writer: BufferedWriter);
}

class RangingServer {
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    fun startServer(port: Int = 8888, listener: ConnectionAcceptedListener? = null) {
        if (serverJob?.isActive == true) {
            return
        }
        serverJob = startServerJob(port, listener)
    }

    fun cancelServer() {
        serverJob?.cancel()
        serverSocket?.close()
        serverJob = null
        serverSocket = null
    }

    private fun startServerJob(port: Int, listener: ConnectionAcceptedListener? = null) = CoroutineScope(Dispatchers.IO).launch {
        try {
            serverSocket = ServerSocket(port)
            Log.d("ServerSocket", "Server is listening on port $port")

            serverSocket?.use {
                while (coroutineContext.isActive) {
                    try {
                        val socket = it.accept()
                        Log.d("Server", "New client connected: ${socket.inetAddress}")
                        launch {
//                            handleConnection(socket)
                            val reader = BufferedReader(InputStreamReader(socket.inputStream))
                            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
                            listener?.onConnectionAccepted(reader, writer)
                        }
                    } catch (e: SocketException) {
                        Log.d("Socket", "Socket is closing due to cancellation.")
                    }
                }
            }

            Log.d("ServerSocket", "Server is closed")
        } catch (e: IOException) {
            Log.e("startServer", "Error: ${e.message}")
            e.printStackTrace()
        }
        Log.d("startServerJob", "job done")
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
            write(writer, "write msg")
        }

        // 等待读写操作完成
        joinAll(readJob, writeJob)
        socket.close()
        Log.d("Server", "Client disconnected: ${socket.inetAddress}")
    }
}







private suspend fun read(reader: BufferedReader) = withContext(Dispatchers.IO) {
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        Log.d("ServerReader", "Received from client: $line")
    }
}

private suspend fun write(writer: BufferedWriter, msg: String) = withContext(Dispatchers.IO) {
    writer.write(msg)
    writer.newLine()
    writer.flush()
    Log.d("ServerWriter", "Sent message to client")
}