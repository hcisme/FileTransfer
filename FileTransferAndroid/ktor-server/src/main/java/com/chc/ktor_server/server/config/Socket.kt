package com.chc.ktor_server.server.config

import android.content.Context
import android.util.Log
import com.chc.ktor_server.server.manage.WebSocketSessionManager
import com.chc.ktor_server.server.manage.WebSocketSessionManager.activeSessionMap
import com.chc.ktor_server.server.manage.WebSocketSessionManager.allClients
import com.chc.ktor_server.server.manage.isAlive
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import org.json.JSONObject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets(context: Context) {
    @Suppress("LocalVariableName")
    val TAG = "WebSocketServer"

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.minutes
    }

    routing {
        webSocket(path = "/") {
            val session = this
            val clientIp = call.request.origin.remoteHost
            var requestId: String? = null
            allClients.add(WebSocketSessionManager.ClientInfo(session = session, ip = clientIp))

            try {
                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()

                            try {
                                val json = JSONObject(text)
                                val type = json.optString("type", "")
                                requestId = json.optString("requestId", "")

                                when (type) {
                                    // 浏览器向手机的传输请求
                                    // 浏览器选择设备 直接连接手机内置的服务
                                    "transfer_request" -> {
                                        activeSessionMap[requestId] =
                                            WebSocketSessionManager.ClientInfo(
                                                session = session,
                                                ip = clientIp
                                            )

                                        // 广播到手机客户端
                                        for (item in allClients) {
                                            if (item.session.isAlive() && (item.ip == "127.0.0.1" || item.ip == "localhost")) {
                                                val obj = JSONObject().apply {
                                                    put("type", "transfer_request")
                                                    put("requestId", requestId)
                                                }
                                                item.session.send(Frame.Text(obj.toString()))
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "${e.message}", e)
                            }
                        }

                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket错误: ${e.message}", e)
            } finally {
                requestId?.let {
                    activeSessionMap.remove(it)
                    Log.d(TAG, "WebSocket会话关闭: $it")
                }
            }
        }
    }
}
