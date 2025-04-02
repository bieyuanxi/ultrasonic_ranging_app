package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Binder
import android.os.IBinder
import android.util.Log

class WifiDirectService : Service() {
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var broadcastReceiver: WiFiDirectBroadcastReceiver

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WifiDirectService = this@WifiDirectService
    }

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        broadcastReceiver = WiFiDirectBroadcastReceiver(manager, channel)

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(broadcastReceiver, intentFilter)
        Log.d("onCreate", "onCreate")
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun getWifiP2pManager(): WifiP2pManager {
        return manager
    }

    fun getWifiP2pChannel(): WifiP2pManager.Channel {
        return channel
    }

    fun setDirectActionListener(directActionListener: DirectActionListener) {
        broadcastReceiver.setDirectActionListener(directActionListener)
    }

    @SuppressLint("MissingPermission")
    fun createGroup() {
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("createGroup", "ok")
            }

            override fun onFailure(reason: Int) {
                Log.d("createGroup", "failure, code: $reason")
            }
        })
    }

    fun removeGroup() {
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("removeGroup", "ok")
            }

            override fun onFailure(reason: Int) {
                Log.d("removeGroup", "failure, code: $reason")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        Log.d("discoverPeers", "try discoverPeers")
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("discoverPeers", "ok")
            }

            override fun onFailure(code: Int) {
                Log.d("discoverPeers", "failure, code: $code")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {

            }

            override fun onFailure(reasonCode: Int) {
                Log.e("WiFiP2P", "连接失败，错误码: $reasonCode")
            }
        })
    }

    fun disconnect() {
        manager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("cancelConnect", "ok")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e("cancelConnect", "code: $reasonCode")
            }
        })
    }
}