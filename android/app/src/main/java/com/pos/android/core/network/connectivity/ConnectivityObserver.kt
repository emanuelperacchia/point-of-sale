package com.pos.android.core.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectivityStatus {
    AVAILABLE, UNAVAILABLE
}

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val connectivity: Flow<ConnectivityStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(ConnectivityStatus.AVAILABLE)
            }

            override fun onLost(network: Network) {
                trySend(ConnectivityStatus.UNAVAILABLE)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                trySend(
                    if (hasInternet) ConnectivityStatus.AVAILABLE
                    else ConnectivityStatus.UNAVAILABLE
                )
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Emitir estado inicial
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(if (isConnected) ConnectivityStatus.AVAILABLE else ConnectivityStatus.UNAVAILABLE)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    fun isCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
