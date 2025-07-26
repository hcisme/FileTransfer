package com.chc.filetransferandroid.utils

import androidx.compose.runtime.compositionLocalOf
import com.chc.filetransferandroid.client.WebSocketClient

/**
 * 路由 NavHostController
 */
val LocalWsClient = compositionLocalOf<WebSocketClient> { error("No WebSocketClient found!") }
