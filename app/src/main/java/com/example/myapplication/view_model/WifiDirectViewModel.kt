package com.example.myapplication.view_model

import android.annotation.SuppressLint
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class WifiDirectViewModel : ViewModel() {
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel

    private val _showToastFlow = MutableSharedFlow<String>()
    val showToastFlow: SharedFlow<String> = _showToastFlow

    private val _wifiDirectState = MutableLiveData<Boolean>()
    val wifiDirectState: LiveData<Boolean> = _wifiDirectState

    private val _peers = MutableSharedFlow<WifiP2pDeviceList>()
    val peers: SharedFlow<WifiP2pDeviceList> = _peers

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _groupOwnerAddress = MutableLiveData<String>()
    val groupOwnerAddress: LiveData<String> = _groupOwnerAddress

    private val _thisDevice = MutableLiveData<WifiP2pDevice>()
    val thisDevice: LiveData<WifiP2pDevice> = _thisDevice

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo

    private val _wifiP2pInfo = MutableLiveData<WifiP2pInfo>()
    val wifiP2pInfo: LiveData<WifiP2pInfo> = _wifiP2pInfo

    fun updateWifiDirectState(enabled: Boolean) {
        _wifiDirectState.value = enabled
    }

    fun updatePeers(peers: WifiP2pDeviceList) {
        viewModelScope.launch {
            _peers.emit(peers)
        }
    }

    fun updateConnectionStatus(networkInfo: NetworkInfo, wifiP2pInfo: WifiP2pInfo, wifiP2pGroup: WifiP2pGroup?) {
        Log.d("", "${networkInfo} \n ${wifiP2pInfo} \n $wifiP2pGroup")
        _networkInfo.value = networkInfo
        _wifiP2pInfo.value = wifiP2pInfo
        if (networkInfo != null && networkInfo.isConnected) {
            _isConnected.value = true
            if (wifiP2pInfo != null && wifiP2pInfo.groupFormed) {
                _groupOwnerAddress.value = wifiP2pInfo.groupOwnerAddress.hostAddress
                wifiP2pInfo.isGroupOwner
            }
        } else {
            _isConnected.value = false
            _groupOwnerAddress.value = ""
        }
//        wifiP2pGroup.
    }



    fun updateThisDevice(wifiP2pDevice: WifiP2pDevice?) {

    }

    fun setWifiP2pComponents(wifiP2pManager: WifiP2pManager, p2pChannel: WifiP2pManager.Channel) {
        manager = wifiP2pManager
        channel = p2pChannel
    }



    // server
    @SuppressLint("MissingPermission")
    fun createGroup() {
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("createGroup", "ok")
                emitText("createGroup: ok")
            }

            override fun onFailure(reason: Int) {
                Log.d("createGroup", "failure, code: $reason")
                emitText("createGroup: failure, code: $reason")
            }
        })
    }

    // server
    fun removeGroup() {
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("removeGroup", "ok")
                emitText("removeGroup: ok")
            }

            override fun onFailure(reason: Int) {
                Log.d("removeGroup", "failure, code: $reason")
                emitText("removeGroup: failure, code: $reason")
            }
        })
    }

    // cli
    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        Log.d("discoverPeers", "try discoverPeers")
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("discoverPeers", "ok")
                emitText("discoverPeers: ok")
            }

            override fun onFailure(code: Int) {
                Log.d("discoverPeers", "failure, code: $code")
                emitText("discoverPeers: failure, code: $code")
            }
        })
    }

    // cli
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("connectToDevice", "ok")
                emitText("connectToDevice: ok")
            }

            override fun onFailure(code: Int) {
                Log.e("WiFiP2P", "failure, code: $code")
                emitText("WiFiP2P: failure, code: $code")
            }
        })
    }

    private fun emitText(text: String) {
        viewModelScope.launch {
            _showToastFlow.emit(text)
        }
    }
}