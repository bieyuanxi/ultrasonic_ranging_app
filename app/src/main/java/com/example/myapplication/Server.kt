package com.example.myapplication

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

class Server {
    private val serverSocketScope = CoroutineScope(Dispatchers.IO)
    private val readerWriterScope = CoroutineScope(Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    private var isServerSocketRunning = false

    fun isServerSocketRunning(): Boolean {
        return isServerSocketRunning
    }

    fun startServerSocket(port: Int = 8888) {
        Log.d("startServerSocket", "Starting server socket...")
        serverSocketScope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("Server", "Server is listening on port $port")
                isServerSocketRunning = true
                while (isActive) {
                    try {
                        val socket = serverSocket?.accept()
                        if (socket != null) {
                            Log.d("Server", "New client connected: ${socket.inetAddress}")
                            // 为每个新连接启动协程处理读写
                            handleConnection(socket)
                        }
                    } catch (e: IOException) {
                        Log.e("error", "Error accepting new connection: ${e.message}")
                        e.printStackTrace()
                    }
                }
            } catch (e: IOException) {
                Log.e("error", "Error starting server socket: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun stopServerSocket() {
        // 取消所有协程
        serverSocketScope.cancel()
        // 关闭 ServerSocket
        try {
            serverSocket?.close()
            isServerSocketRunning = false
            Log.d("Server", "ServerSocket closed")
        } catch (e: IOException) {
            Log.e("error", "Error closing ServerSocket: ${e.message}")
            e.printStackTrace()
        }

    }

    fun closeSocket(socket: Socket) {
        try {
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 处理单个连接的读写
    private fun handleConnection(socket: Socket) {
        // 启动协程处理读操作
        readerWriterScope.launch {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
            try {
                var line: String? = ""
                while (reader.readLine().also { line = it } != null) {
                    Log.d("ServerReader", "Received from client: $line")
                    write(writer, "Pong")
                }
            } catch (e: IOException) {
                Log.e("ServerReader", "Error reading from client: ${e.message}")
                e.printStackTrace()
            } finally {
                try {
                    socket.close()
                    Log.d("ServerReader", "Socket closed: ${socket.inetAddress}")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // TODO: 只有连接建立后才可调用
    private fun write(writer: BufferedWriter, msg: String) {
        readerWriterScope.launch {
            try {
                // 发送数据
                writer.write(msg)
                writer.newLine()
                writer.flush()
                Log.d("ServerWriter", "Sent message to client")
            } catch (e: IOException) {
                Log.e("ServerWriter", "Error writing to client: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}