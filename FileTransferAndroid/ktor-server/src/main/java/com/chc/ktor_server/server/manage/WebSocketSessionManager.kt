package com.chc.ktor_server.server.manage

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import java.util.Collections

object WebSocketSessionManager {
    val activeSessionMap = mutableMapOf<String, ClientInfo>()

    val allClients = Collections.synchronizedSet(mutableSetOf<ClientInfo>())

    data class ClientInfo(
        val session: WebSocketSession,
        val ip: String,
        var lastActive: Long = System.currentTimeMillis()
    )
}

suspend fun WebSocketSession.isAlive(): Boolean {
    return try {
        this.send(Frame.Ping(byteArrayOf()))
        true
    } catch (_: Exception) {
        false
    }
}
