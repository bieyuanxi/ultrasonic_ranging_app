package com.example.myapplication.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class ClientViewModel : ViewModel() {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    // 定义一个 MutableLiveData 用于存储接收到的消息
    private val _receivedMessage = MutableLiveData<String>()
    // 提供一个只读的 LiveData 供外部观察
    val receivedMessage: LiveData<String> = _receivedMessage

    fun connectToServer(ip: String, port: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                socket = Socket(ip, port)
                writer = socket?.outputStream?.let { PrintWriter(it, true) }
                reader = BufferedReader(InputStreamReader(socket?.inputStream))

                // 开始接收服务器消息
                receiveMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun receiveMessages() = withContext(Dispatchers.IO) {
        try {
            var message: String?
            while (socket?.isConnected == true && reader != null) {
                message = reader?.readLine()
                if (message != null) {
                    // 在主线程更新 LiveData
                    viewModelScope.launch(Dispatchers.Main) {
                        _receivedMessage.value = message
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            writer?.println(message)
        }
    }

    fun stopSocket() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                socket?.close()
                writer?.close()
                reader?.close()
                socket = null
                writer = null
                reader = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSocket()
    }
}