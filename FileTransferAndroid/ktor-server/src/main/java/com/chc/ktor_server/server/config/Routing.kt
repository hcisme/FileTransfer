package com.chc.ktor_server.server.config

import android.content.Context
import android.os.Build
import android.util.Log
import com.chc.ktor_server.server.KtorServer
import com.chc.ktor_server.server.manage.WebSocketSessionManager.activeSessionMap
import com.chc.ktor_server.utils.saveToPublicDownloads
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.websocket.Frame
import org.json.JSONObject
import java.util.Date

fun Application.configureRouting(context: Context) {
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
            val multipart = call.receiveMultipart(formFieldLimit = KtorServer.Companion.UPLOAD_FILE_LIMIT)

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
                Log.e(KtorServer.Companion.TAG, e.message.toString())
                val responseText = """
                            {
                                "code": 500
                            }
                        """.trimIndent()
                call.respondText(text = responseText)
            }
        }

        // 本机调用
        post("/api/transfer/response") {
            val requestBody = call.receiveText()

            val json = JSONObject(requestBody)
            val requestId = json.getString("requestId")
            val accepted = json.getBoolean("accepted")

            val activeSession = activeSessionMap[requestId]
            if (activeSession == null) {
                // 发送404 然后不往下执行
                return@post
            }

            val obj = JSONObject().apply {
                put("type", "transfer_response")
                put("requestId", requestId)
                put("accepted", accepted)
            }
            // 直接发送到浏览器 ws
            activeSession.session.send(Frame.Text(obj.toString()))
        }
    }
}