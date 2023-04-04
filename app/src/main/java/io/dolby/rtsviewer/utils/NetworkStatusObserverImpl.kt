package io.dolby.rtsviewer.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class NetworkStatusObserverImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkStatusObserver {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun connectedStatus(): NetworkStatusObserver.Status {
        return if (connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasCapability(NET_CAPABILITY_INTERNET) == true) {
            NetworkStatusObserver.Status.Available
        } else {
            NetworkStatusObserver.Status.Unavailable
        }
    }

    override val status: Flow<NetworkStatusObserver.Status>
        get() = callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(NetworkStatusObserver.Status.Available) }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    launch { send(NetworkStatusObserver.Status.Unavailable) }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    launch { send(NetworkStatusObserver.Status.Unavailable) }
                }
            }

            launch { send(connectedStatus()) }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
}
