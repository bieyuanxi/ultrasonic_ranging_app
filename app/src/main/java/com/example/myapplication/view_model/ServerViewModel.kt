package com.example.myapplication.view_model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap

class ServerViewModel : ViewModel() {
    private var serverJob: kotlinx.coroutines.Job? = null
    private var serverSocket: ServerSocket? = null
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning

    val connectedSockets = ConcurrentHashMap<String, Socket>()
    private val _connectedSocketInfo = MutableStateFlow<List<String>>(emptyList())
    val connectedSocketInfo: StateFlow<List<String>> = _connectedSocketInfo

    fun startServer(port: Int = 8888) {
        if(isServerRunning.value) {
            Log.d("Server", "isServerRunning=true")
            return;
        }
        serverJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                _isServerRunning.value = true
                while (isActive) {
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isServerRunning.value = false
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverSocket?.close()
        _isServerRunning.value = false
    }

    private fun handleClient(socket: Socket) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                connectedSockets[socket.toString()] = socket
                updateConnectedSocketInfo()

                val input = BufferedReader(InputStreamReader(socket.inputStream))
                val output = PrintWriter(socket.outputStream, true)

                while (true) {
                    val message = input.readLine()
                    // 处理客户端消息
                    Log.d("ServerReceived", "Received: $message")
                    // 发送响应给客户端
                    output.println("Message received: $message")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket.close()
                connectedSockets.remove(socket.toString())
                updateConnectedSocketInfo()
            }
        }
    }

    private fun updateConnectedSocketInfo() {
        val info = connectedSockets.map { (index, socket) ->
            "Socket Index: $index, Remote Address: ${socket.inetAddress.hostAddress}"
        }
        _connectedSocketInfo.value = info
    }
}