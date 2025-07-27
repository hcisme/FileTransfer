package com.chc.filetransferandroid.client

import android.util.Log
import com.chc.filetransferandroid.utils.JmDNSDeviceDiscovery
import com.chc.ktor_server.utils.SERVICE_PORT
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 手机和手机服务建立 ws
 */
class WebSocketServer {
    private val client = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).build()
    private var ws: WebSocket? = null
    private var requestListener: RequestListener? = null

    init {
        val localWsServerUrl = "ws://127.0.0.1:${SERVICE_PORT}/"
        val request = Request.Builder().url(url = localWsServerUrl).build()
        ws = client.newWebSocket(request, Listener())
    }

    interface RequestListener {
        fun onTransferRequest(requestId: String)
    }

    fun setRequestListener(listener: RequestListener) {
        this.requestListener = listener
    }

    fun sendMessage(requestId: String, type: String = "transfer_response") {
        val json = JSONObject().apply {
            put("type", type)
            put("requestId", requestId)
        }
        ws?.send(json.toString())
    }

    private inner class Listener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)

                when (json.getString("type")) {
                    "transfer_request" -> {
                        val requestId = json.getString("requestId")
                        requestListener?.onTransferRequest(requestId = requestId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "消息解析失败: ${e.message}")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {}

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "连接失败", t)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "连接关闭: $reason")
        }
    }

    companion object {
        private const val TAG = "WebSocketServer"
    }
}