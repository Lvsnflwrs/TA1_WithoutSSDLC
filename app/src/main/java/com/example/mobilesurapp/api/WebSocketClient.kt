package com.example.mobilesurapp.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val TAG = "AppWebSocketService"
    var webSocket: WebSocket? = null

    private val _incomingMessages = Channel<String>(Channel.UNLIMITED)
    val incomingMessages: Flow<String> = _incomingMessages.receiveAsFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun connect(wsUrl: String) {
        if (webSocket != null && _isConnected.value) return

        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isConnected.value = true
                Log.d(TAG, "Connected to $wsUrl")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _incomingMessages.trySend(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                this@WebSocketClient.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isConnected.value = false
                this@WebSocketClient.webSocket = null
                Log.e(TAG, "Connection Failure: ${t.message}")
            }
        })
    }

    suspend fun send(message: String): Boolean {
        if (!_isConnected.value) {
            withTimeoutOrNull(5000L) {
                _isConnected.first { it }
            }
        }

        val sent = webSocket?.send(message) ?: false
        if (sent) Log.d(TAG, "Sent: $message")
        return sent
    }

    fun logout() {
        webSocket?.close(1000, "User Logout")
        _isConnected.value = false
        webSocket = null
    }
}