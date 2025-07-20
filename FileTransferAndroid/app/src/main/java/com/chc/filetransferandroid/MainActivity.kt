package com.chc.filetransferandroid

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import com.chc.filetransferandroid.server.KtorServer
import com.chc.filetransferandroid.ui.screen.UploadView
import com.chc.filetransferandroid.ui.theme.FileTransferAndroidTheme
import com.chc.filetransferandroid.utils.JmDNSDeviceDiscovery

class MainActivity : ComponentActivity() {
    private lateinit var jm: JmDNSDeviceDiscovery
    private lateinit var ktorServer: KtorServer

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            0
        )

        ktorServer = KtorServer(applicationContext).apply {
            start()
        }

        jm = JmDNSDeviceDiscovery(this).apply {
            startDiscoveryAndPublish()
        }

        setContent {
            FileTransferAndroidTheme(
                dynamicColor = false
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    UploadView(jm = jm)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val insetsControllerCompat = WindowInsetsControllerCompat(window, window.decorView)
        val nightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK

        when (nightMode) {
            Configuration.UI_MODE_NIGHT_YES -> {
                insetsControllerCompat.apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }

            Configuration.UI_MODE_NIGHT_NO -> {
                insetsControllerCompat.apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }

            else -> {}
        }
    }
}
