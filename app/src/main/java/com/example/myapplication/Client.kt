package com.example.myapplication

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class Client {
    private val clientSocketScope = CoroutineScope(Dispatchers.IO)
    private val readerWriterScope = CoroutineScope(Dispatchers.IO)
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    private var isClientSocketRunning = false

    fun isClientSocketRunning(): Boolean {
        return isClientSocketRunning
    }

    // 启动客户端连接
    fun startClient(host: String, port: Int = 8888) {
        clientSocketScope.launch {
            Log.d("Client", "Starting client connection...")
            try {
                socket = Socket(host, port)
                reader = BufferedReader(InputStreamReader(socket?.inputStream))
                writer = BufferedWriter(OutputStreamWriter(socket?.outputStream))
                isClientSocketRunning = true
                Log.d("Client", "Connected to server: ${socket?.inetAddress}")
                handleConnection()
            } catch (e: IOException) {
                Log.e("Client", "Error connecting to server: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun stopClient() {
        clientSocketScope.cancel()
        // 关闭 Socket
        try {
            isClientSocketRunning = false
            socket?.close()
            Log.d("Client", "Socket closed")
        } catch (e: IOException) {
            Log.e("Client", "Error closing socket: ${e.message}")
            e.printStackTrace()
        }
    }

    // TODO: 只有连接建立后才可调用
    fun write(string: String) {
        readerWriterScope.launch {
            try {
                // 发送数据
                writer?.write(string)
                writer?.newLine()
                writer?.flush()
                Log.d("ClientWriter", "Sent message to server")
            } catch (e: IOException) {
                Log.e("ClientWriter", "Error writing to server: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // 处理连接的读写
    private fun handleConnection() {
        readerWriterScope.launch {
            try {
                var line: String?
                while (reader?.readLine().also { line = it } != null) {
                    Log.d("ClientReader", "Received from server: $line")
                }
            } catch (e: IOException) {
                Log.e("ClientReader", "Error reading from server: ${e.message}")
                e.printStackTrace()
            } finally {
                try {
                    socket?.close()
                    Log.d("ClientReader", "Socket closed")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }


}