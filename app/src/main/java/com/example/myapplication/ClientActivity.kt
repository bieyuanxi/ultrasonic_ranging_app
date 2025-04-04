package com.example.myapplication

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.view_model.ClientViewModel
import com.example.myapplication.view_model.ServerViewModel
import com.example.myapplication.view_model.WifiDirectViewModel

class ClientActivity : ComponentActivity() {
    private lateinit var receiver: WiFiDirectBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = wifiP2pManager.initialize(this, mainLooper, null)

        val clientViewModel: ClientViewModel by viewModels()
        val wifiDirectViewModel: WifiDirectViewModel by viewModels()

        wifiDirectViewModel.setWifiP2pComponents(wifiP2pManager, channel);

        receiver = WiFiDirectBroadcastReceiver(wifiP2pManager, channel, wifiDirectViewModel)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                wifiDirectViewModel.showToastFlow.collect {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }

            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting2(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, receiver.getIntentFilter())
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    val clientViewModel: ClientViewModel = viewModel()
    val wifiDirectViewModel: WifiDirectViewModel = viewModel()

    val wifiDirectState by wifiDirectViewModel.wifiDirectState.observeAsState(false)
    val isConnected by wifiDirectViewModel.isConnected.observeAsState(false)
    val groupOwnerAddress by wifiDirectViewModel.groupOwnerAddress.observeAsState("")
    if (isConnected) {
        clientViewModel.connectToServer(groupOwnerAddress, 8888)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello ClientActivity, wifiDirectEnabled=${wifiDirectState}"
        )
        Button(
            onClick = {
                wifiDirectViewModel.discoverPeers()
            }
        ) {
            Text(text = "Discover Peers")
        }

        Button(
            onClick = {
                clientViewModel.sendMessage("Hello from Cli")
            }
        ) {
            Text(text = "Send Msg")
        }

        val deviceList by wifiDirectViewModel.peers.collectAsStateWithLifecycle(
            initialValue = WifiP2pDeviceList()
        )

        Column {
            Text(text = "Wifi P2P Devices, click to connect")
            LazyColumn {
                items(deviceList.deviceList.toList()) { device: WifiP2pDevice ->
                    Button(onClick = {
                        wifiDirectViewModel.connectToDevice(device)
                    }) {
                        Text(text = device.deviceName)
                    }
                }
            }
        }

    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    MyApplicationTheme {
        Greeting2("Android")
    }
}