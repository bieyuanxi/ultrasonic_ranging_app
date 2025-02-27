package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme


// https://developer.android.google.cn/develop/connectivity/wifi/wifip2p?hl=zh-cn#discover-peers
class WifiP2P : ComponentActivity() {
    val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }

    var channel: WifiP2pManager.Channel? = null

    private val peers = mutableStateListOf<WifiP2pDevice>()
    private var isWifiP2pEnabled by mutableStateOf(false)
    private var selectedDevice: WifiP2pDevice? by mutableStateOf(null)

    private val text = mutableStateOf("")
    private val groupList = mutableStateListOf<WifiP2pDevice>()

    private val server: Server by lazy(LazyThreadSafetyMode.NONE) {
        Server()
    }
    private val client: Client by lazy(LazyThreadSafetyMode.NONE) {
        Client()
    }


    private val wifiP2pReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission", "NewApi")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {   // 当 WLAN P2P 在设备上启用或停用时广播。
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    isWifiP2pEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                    Log.d("STATE_CHANGED", "$state")
                }
                // 在调用 discoverPeers() 时广播。如果您在应用中处理此 intent，则通常需要调用 requestPeers() 以获取对等设备的更新列表。
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d("PEERS_CHANGED", "")
                    manager?.requestPeers(channel) { peerList ->
                        Log.d("WifiP2pDeviceList", "$peerList")
                        peers.clear()
                        peers.addAll(peerList.deviceList)
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {  // 当设备的 WLAN 连接状态更改时广播
                    // 处理连接变化
                    Log.d("CONNECTION_STATE_CHANGE", "")

//                    manager?.requestConnectionInfo(channel) {
//                        val isGroupOwner = it.isGroupOwner
//                        val addr = it.groupOwnerAddress
//                        Log.d("is Group Owner?", "$isGroupOwner")
//                        Log.d("address of GO?", "$addr")
//                        it.groupFormed
//                        if(isGroupOwner) {
//
//                        } else {
//
//                        }
//                    }


                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> { // 当设备的详细信息（例如设备名称）更改时广播。
                    // 处理本设备信息变化
                    Log.d("THIS_DEVICE_CHANGED", "")
                }
            }
        }
    }

    private val requestDiscoverPeersPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        Log.d("allPermissionsGranted", "$allPermissionsGranted")
        if (allPermissionsGranted) {

        } else {
            // 部分权限被拒绝，处理相应逻辑
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()

        Log.d("FEATURE_WIFI_DIRECT", "${packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)}")

        channel = manager?.initialize(this, mainLooper, null)

        checkAndRequestPermissions()

        discoverPeers()

        checkExistingWifiP2pConnection()

        setContent {
//            MyApplicationTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "连接状态： ${text.value}")
                Text("已连接设备列表")
                groupList.forEach { device ->
                    Button(onClick = { client.write("Pong") }) {
                        Text(text = device.deviceName)
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Button(
                    onClick = { discoverPeers() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "发现设备")
                }
                peers.forEach { device ->
                    Button(onClick = { connectToDevice(device) }) {
                        Text(device.deviceName)
                    }
                }
            }
        }



    }

    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(wifiP2pReceiver, intentFilter)
        Log.d("registerReceiver", "")
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiP2pReceiver)
    }

    @SuppressLint("MissingPermission")
    private fun checkExistingWifiP2pConnection() {
        manager?.requestConnectionInfo(channel) {
            Log.d("checkExistingWifiP2pConn", "${it.groupFormed}")
            text.value = "groupFormed=${it.groupFormed}, isGroupOwner=${it.isGroupOwner}"
            if (it.groupFormed) {
                if(it.isGroupOwner) {
                    if (!server.isServerSocketRunning()) {
                        server.startServerSocket()
                    }
                    manager?.requestGroupInfo(channel) { group ->
                        Log.d("ImServer", "clientList: ${group.clientList}")
                        groupList.clear()
                        groupList.addAll(group.clientList)
                    }
                } else {
                    if (!client.isClientSocketRunning()) {
                        it.groupOwnerAddress.hostAddress?.let { it1 -> client.startClient(it1) }
                    }
//                    client.write("Ping")

                    manager?.requestGroupInfo(channel) { group ->
                        Log.d("ImClient", "Group Owner: ${group.owner}")
                        groupList.clear()
                        groupList.addAll(listOf(group.owner))
                    }
                }

            }
        }


    }


    @SuppressLint("MissingPermission")
    private fun discoverPeers() {
        Log.d("discoverPeers", "try discoverPeers")
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
//                ...
                Log.d("discoverPeers", "success")
            }

            override fun onFailure(reasonCode: Int) {
                Log.d("discoverPeers", "failure, code: ${reasonCode}")
            }
        })
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    // >= android 13
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }else {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestDiscoverPeersPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun createGroup() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Device is ready to accept incoming connections from peers.
            }

            override fun onFailure(reason: Int) {
                Log.d("createGroup", "failure, code: ${reason}")
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress

        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                manager?.requestConnectionInfo(channel) {
                    Log.d("WiFiP2P", "is groupFormed: ${it.groupFormed}")
                    text.value = "groupFormed=${it.groupFormed}, isGroupOwner=${it.isGroupOwner}"
                    if (it.groupFormed) {
                        val isGroupOwner = it.isGroupOwner
                        val addr = it.groupOwnerAddress
                        Log.d("is Group Owner?", "$isGroupOwner")
                        Log.d("address of GO?", "$addr")

                        if(isGroupOwner) {
                            if (!server.isServerSocketRunning()) {
                                server.startServerSocket()
                            }
                        } else {
                            if (!client.isClientSocketRunning()) {
                                it.groupOwnerAddress.hostAddress?.let { it1 -> client.startClient(it1) }
                            }
//                            client.write("Ping")
                        }
                    }

                }
            }

            override fun onFailure(reasonCode: Int) {
                Log.e("WiFiP2P", "连接失败，错误码: $reasonCode")
            }
        })
    }

//    private fun startServerSocket() {
//        Log.d("startServerSocket", "")
//        Thread {
//            try {
//                val serverSocket = ServerSocket(8888)
//                val socket = serverSocket.accept()
//                val reader = BufferedReader(InputStreamReader(socket.inputStream))
//                val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
//                Log.d("AAAAAAAAAAAA", "")
//                // 发送数据
//                Log.d("server", "")
//                writer.write("Hello from server!")
//                writer.newLine()
//                writer.flush()
//            } catch (e: IOException) {
//                Log.d("error", "")
//                e.printStackTrace()
//            }
//        }.start()
//    }
//
//    private fun startClientSocket(groupOwnerAddress: String) {
//        Thread {
//            try {
//                Log.d("cli", groupOwnerAddress)
//                val socket = Socket(groupOwnerAddress, 8888)
//
//                val reader = BufferedReader(InputStreamReader(socket.inputStream))
//                val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
//                // 接收数据
//                val message = reader.readLine()
//                Log.d("WiFiP2P", "Received from server: $message")
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }.start()
//    }






}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}