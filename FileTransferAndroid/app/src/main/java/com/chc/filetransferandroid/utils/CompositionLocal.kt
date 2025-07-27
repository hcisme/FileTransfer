package com.chc.filetransferandroid.utils

import androidx.compose.runtime.compositionLocalOf
import com.chc.filetransferandroid.client.WebSocketClient
import com.chc.filetransferandroid.client.WebSocketServer


val LocalWsClient = compositionLocalOf<WebSocketClient> { error("No WebSocketClient found!") }

val LocalWsServer = compositionLocalOf<WebSocketServer> { error("No WebSocketServer found!") }
