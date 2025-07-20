package com.chc.filetransferandroid.server

import android.content.Context
import android.os.Build
import android.util.Log
import com.chc.filetransferandroid.utils.JmDNSDeviceDiscovery
import com.chc.filetransferandroid.utils.saveToPublicDownloads
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date

class KtorServer(private val context: Context) {
    private val serverScope = CoroutineScope(Dispatchers.IO)

    private val server by lazy {
        embeddedServer(CIO, port = JmDNSDeviceDiscovery.SERVICE_PORT) { module() }
    }

    fun Application.module() {
        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
        }

        routing {
            get("/") {
                call.respondText(
                    "手机型号 ${Build.MODEL} 运行正常",
                    ContentType.Text.Plain
                )
            }

            post("/upload") {
                val multipart = call.receiveMultipart(formFieldLimit = UPLOAD_FILE_LIMIT)

                try {
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                saveToPublicDownloads(
                                    context = context,
                                    fileName = "${Date().time}-${part.originalFileName}",
                                    part = part
                                )

                                part.dispose()
                            }

                            else -> part.dispose()
                        }
                    }

                    val responseText = """
                            {
                                "code": 200
                            }
                        """.trimIndent()
                    call.respondText(text = responseText)
                } catch (e: Exception) {
                    Log.e(TAG, e.message.toString())
                    val responseText = """
                            {
                                "code": 500
                            }
                        """.trimIndent()
                    call.respondText(text = responseText)
                }
            }
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
        private const val TAG = "KtorServer"

        /**
         * 1KB = 1024bytes
         *
         * 1MB = 1024 * 1024
         *
         * 200MB
         */
        private const val UPLOAD_FILE_LIMIT = 200L * 1024 * 1024
    }
}
