package com.example.myapplication

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.compose.runtime.mutableStateListOf

interface DirectActionListener : WifiP2pManager.ChannelListener {
    fun onWifiP2pStateChanged(enabled: Boolean)

    fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo)

    fun onDisconnection()

    fun onSelfDeviceAvailable(device: WifiP2pDevice)

    fun onPeersAvailable(devices: List<WifiP2pDevice>)
}

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val directActionListener: DirectActionListener
) : BroadcastReceiver() {
    private val TAG = "WiFiDirectBroadcastReceiver"

    private val peerList = mutableStateListOf<WifiP2pDevice>()

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val enabled = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE,
                    -1
                ) == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                directActionListener.onWifiP2pStateChanged(enabled)
                if (!enabled) {
                    directActionListener.onPeersAvailable(emptyList())
                }
                Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION： $enabled")
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION")
                manager.requestPeers(channel) { peers ->
                    directActionListener.onPeersAvailable(peers.deviceList.toList())
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo =
                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION ： " + networkInfo?.isConnected)
                if (networkInfo != null && networkInfo.isConnected) {
                    manager.requestConnectionInfo(channel) { info ->
                        if (info != null) {
                            directActionListener.onConnectionInfoAvailable(info)
                        }
                    }
                    Log.d(TAG, "已连接 P2P 设备")
                } else {
                    directActionListener.onDisconnection()
                    Log.d(TAG, "与 P2P 设备已断开连接")
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val wifiP2pDevice =
                    intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                if (wifiP2pDevice != null) {
                    directActionListener.onSelfDeviceAvailable(wifiP2pDevice)
                }
                Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ： ${wifiP2pDevice.toString()}")
            }
        }
    }
}