package com.smartsystems.chatapp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Network connectivity monitor using modern Android ConnectivityManager API.
 * Provides real-time network state updates via Kotlin Flow.
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager = ContextCompat.getSystemService(
        context,
        ConnectivityManager::class.java
    ) ?: throw IllegalStateException("ConnectivityManager not available")

    private val _networkState = MutableStateFlow(isConnectedAtStartup())
    val networkState: StateFlow<Boolean> = _networkState.asStateFlow()

    val isConnected: Boolean
        get() = _networkState.value

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkState.value = true
        }

        override fun onLost(network: Network) {
            _networkState.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            val isValidated = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
            _networkState.value = hasInternet && isValidated
        }
    }

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun isConnectedAtStartup(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Callback already unregistered
        }
    }
}
