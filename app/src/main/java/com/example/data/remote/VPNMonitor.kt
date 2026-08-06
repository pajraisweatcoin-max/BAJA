package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.util.AppLogger
import com.example.util.ConnectionLogger
import com.example.util.ToastManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VPNMonitor private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val toastManager = ToastManager(appContext)
    private val logger = ConnectionLogger(appContext)

    private val _isVpnConnected = MutableStateFlow(false)
    val isVpnConnected: StateFlow<Boolean> = _isVpnConnected.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var vpnCallback: ConnectivityManager.NetworkCallback? = null
    private var defaultNetworkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        @Volatile
        private var instance: VPNMonitor? = null

        fun getInstance(context: Context): VPNMonitor {
            return instance ?: synchronized(this) {
                instance ?: VPNMonitor(context.applicationContext).also { instance = it }
            }
        }
    }

    fun startMonitoring(
        onVpnConnected: () -> Unit = {},
        onVpnDisconnected: () -> Unit = {}
    ) {
        if (vpnCallback != null) return // Already monitoring

        try {
            // Monitor VPN Network
            val vpnRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build()

            vpnCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    AppLogger.s("VPN_MONITOR", "VPN Network Interface OnAvailable")
                    _isVpnConnected.value = true
                    logger.log("VPN Connected", "Antarmuka VPN aktif")
                    toastManager.showToast("VPN Connected")
                    onVpnConnected()
                }

                override fun onLost(network: Network) {
                    AppLogger.w("VPN_MONITOR", "VPN Network Interface OnLost")
                    _isVpnConnected.value = false
                    logger.log("VPN Disconnected", "Antarmuka VPN terputus")
                    toastManager.showToast("VPN Lost")
                    onVpnDisconnected()
                }
            }

            connectivityManager.registerNetworkCallback(vpnRequest, vpnCallback!!)

            // Monitor Default Network
            defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkAvailable.value = true
                }

                override fun onLost(network: Network) {
                    _isNetworkAvailable.value = false
                }
            }
            connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback!!)

            AppLogger.i("VPN_MONITOR", "VPN & NetworkCallback registered")
            checkInitialVpnState()
        } catch (e: Exception) {
            AppLogger.e("VPN_MONITOR", "Gagal mendaftarkan NetworkCallback: ${e.message}")
        }
    }

    fun stopMonitoring() {
        vpnCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
            vpnCallback = null
        }
        defaultNetworkCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
            defaultNetworkCallback = null
        }
        AppLogger.i("VPN_MONITOR", "VPN Monitoring dihentikan")
    }

    fun checkInitialVpnState(): Boolean {
        return try {
            val networks = connectivityManager.allNetworks
            var vpnFound = false
            for (network in networks) {
                val caps = connectivityManager.getNetworkCapabilities(network)
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    vpnFound = true
                    break
                }
            }
            _isVpnConnected.value = vpnFound
            vpnFound
        } catch (e: Exception) {
            AppLogger.e("VPN_MONITOR", "Gagal memeriksa status VPN awal: ${e.message}")
            false
        }
    }
}
