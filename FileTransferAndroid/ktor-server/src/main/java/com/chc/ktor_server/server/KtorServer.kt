package com.chc.ktor_server.server

import android.content.Context
import com.chc.ktor_server.server.config.configureRouting
import com.chc.ktor_server.server.config.configureSockets
import com.chc.ktor_server.utils.SERVICE_PORT
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

class KtorServer(private val context: Context) {

    private val server by lazy {
        embeddedServer(CIO, port = SERVICE_PORT) {
            configureRouting(context)
            configureSockets(context)
        }
    }

    /** 启动服务器 */
    fun start() {
        server.start(wait = false)
    }

    /** 停止服务器 */
    fun stop() {
        server.stop(1_000, 2_000)
    }

    companion object {
        const val TAG = "KtorServer"

        /**
         * 1KB = 1024bytes
         *
         * 1MB = 1024 * 1024
         *
         * 200MB
         */
        const val UPLOAD_FILE_LIMIT = 200L * 1024 * 1024
    }
}
