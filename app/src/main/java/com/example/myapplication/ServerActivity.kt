package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.view_model.ServerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.sound.AudioRecordManager
import com.example.myapplication.sound.AudioTrackManager
import com.example.myapplication.view_model.WifiDirectViewModel
import kotlinx.coroutines.flow.collect

class ServerActivity : ComponentActivity() {
    private var audioRecordManager = AudioRecordManager()
    private val audioTrackManager = AudioTrackManager()

    private lateinit var receiver: WiFiDirectBroadcastReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = wifiP2pManager.initialize(this, mainLooper, null)

        val serverViewModel: ServerViewModel by viewModels()
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
                    Greeting3(
                        modifier = Modifier.padding(innerPadding)
                    )


                }
            }


        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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
fun Greeting3(modifier: Modifier = Modifier) {
    val serverViewModel: ServerViewModel = viewModel()
    val wifiDirectViewModel: WifiDirectViewModel = viewModel()

    val connectedList by serverViewModel.connectedSocketInfo.collectAsState()
    val wifiDirectState = wifiDirectViewModel.wifiDirectState.observeAsState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello ServerActivity, wifiDirectState=${wifiDirectState.value}"
        )
        Button(
            onClick = {
                wifiDirectViewModel.createGroup()
                serverViewModel.startServer()
            }
        ) {
            Text(text = "Create Group")
        }
        Button(
            onClick = {
                wifiDirectViewModel.removeGroup()
                serverViewModel.stopServer()
            }
        ) {
            Text(text = "Remove Group")
        }
        Button(
            onClick = {}
        ) {
            Text(text = "Start Ranging")
        }
        Text(text = "Socket Client count: ${connectedList.size}")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview3() {
    MyApplicationTheme {
        Greeting3()
    }
}