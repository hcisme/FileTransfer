package com.chc.filetransferandroid.client

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketClient {
    private val client = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).build()
    private var ws: WebSocket? = null
    private var currentWsUrl: String? = null
    private var responseListener: ResponseListener? = null

    interface ResponseListener {
        fun onTransferResponse(requestId: String, accepted: Boolean)
    }

    fun init(wsUrl: String): WebSocketClient {
        if (currentWsUrl == wsUrl) {
            return this
        }
        ws?.close(1000, "Reconnecting")
        ws = null

        val request = Request.Builder().url(url = wsUrl).build()
        ws = client.newWebSocket(request, Listener())
        return this
    }

    fun setResponseListener(listener: ResponseListener) {
        this.responseListener = listener
    }

    fun sendMessage(requestId: String, type: String = "transfer_request") {
        val json = JSONObject().apply {
            put("type", type)
            put("requestId", requestId)
        }
        ws?.send(json.toString())
    }

    private inner class Listener : WebSocketListener() {
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {}

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {}

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {}

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)

                when (json.getString("type")) {
                    "transfer_response" -> {
                        val requestId = json.getString("requestId")
                        val accepted = json.getBoolean("accepted")
                        responseListener?.onTransferResponse(requestId, accepted)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "消息解析失败: ${e.message}")
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}

        override fun onOpen(webSocket: WebSocket, response: Response) {}
    }

    companion object {
        private const val TAG = "WebSocketClient"
    }
}