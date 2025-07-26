package com.chc.filetransferandroid.server

import android.content.Context
import com.chc.filetransferandroid.utils.JmDNSDeviceDiscovery
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KtorServer(private val context: Context) {
    private val serverScope = CoroutineScope(Dispatchers.IO)

    private val server by lazy {
        embeddedServer(CIO, port = JmDNSDeviceDiscovery.SERVICE_PORT) {
            configureRouting(context)
        }
    }

    /** 启动服务器 */
    fun start() {
        serverScope.launch { server.start(true) }
    }

    /** 停止服务器 */
    fun stop() {
        server.stop(1_000, 2_000)
        serverScope.cancel()
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
