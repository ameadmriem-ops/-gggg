package com.example.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Real-time network status manager that monitors connectivity changes
 * and provides exponential backoff and timeout-guarded execution.
 */
class NetworkManager private constructor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    Log.d("NetworkManager", "Network connection established")
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkInitialConnectivity()
                    Log.w("NetworkManager", "Network connection lost")
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    _isOnline.value = hasInternet
                }
            })
        } catch (e: Exception) {
            Log.w("NetworkManager", "Network callback registration note: ${e.message}")
            _isOnline.value = true
        }
    }

    companion object {
        @Volatile
        private var instance: NetworkManager? = null

        fun getInstance(context: Context): NetworkManager {
            return instance ?: synchronized(this) {
                instance ?: NetworkManager(context).also { instance = it }
            }
        }

        /**
         * Executes an async block with exponential backoff and timeout protection.
         */
        suspend fun <T> executeWithRetry(
            maxRetries: Int = 3,
            initialDelayMs: Long = 1000L,
            maxDelayMs: Long = 5000L,
            timeoutMs: Long = 15000L,
            tag: String = "NetworkCall",
            block: suspend () -> T
        ): Result<T> {
            var currentDelay = initialDelayMs
            var lastException: Throwable = Exception("Unknown network error")

            for (attempt in 1..maxRetries) {
                try {
                    val result = withTimeoutOrNull(timeoutMs) {
                        block()
                    }
                    if (result != null) {
                        return Result.success(result)
                    } else {
                        lastException = Exception("انتهت مهلة الطلب (Timeout)، يرجى التحقق من سرعة الاتصال")
                    }
                } catch (e: Throwable) {
                    lastException = e
                    Log.w(tag, "Attempt $attempt failed: ${e.message}")
                }

                if (attempt < maxRetries) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
                }
            }
            return Result.failure(lastException)
        }
    }
}
