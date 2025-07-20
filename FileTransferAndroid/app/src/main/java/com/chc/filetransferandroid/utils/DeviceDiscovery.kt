package com.chc.filetransferandroid.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlin.coroutines.cancellation.CancellationException

class JmDNSDeviceDiscovery(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var jmdns: JmDNS? = null
    private var publishedService: ServiceInfo? = null
    private val _discoveredDevices = MutableStateFlow<List<ServiceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<ServiceInfo>> = _discoveredDevices

    private val serviceListener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) {
            Log.d(TAG, "服务添加: ${event.name}")
            scope.launch {
                try {
                    jmdns?.requestServiceInfo(event.type, event.name, true)
                } catch (e: Exception) {
                    Log.e(TAG, "请求服务信息失败: ${e.message}")
                }
            }
        }

        override fun serviceRemoved(event: ServiceEvent) {
            Log.d(TAG, "服务移除: ${event.name}")
            _discoveredDevices.update { services ->
                services.filter { it.name != event.name }
            }
        }

        override fun serviceResolved(event: ServiceEvent) {
            val serviceInfo = event.info

            // 过滤自身服务
            if (serviceInfo.name == SERVICE_NAME) {
                Log.d(TAG, "忽略自身服务: ${serviceInfo.name}")
                return
            }

            // 过滤重复解析
            if (_discoveredDevices.value.any { it.name == serviceInfo.name }) {
                Log.d(TAG, "服务已存在: ${serviceInfo.name}")
                return
            }

            val ip = serviceInfo.inetAddresses.firstOrNull()?.hostAddress ?: "未知IP"
            Log.i(TAG, "服务解析成功: ${serviceInfo.name} | IP: $ip | 端口: ${serviceInfo.port}")

            _discoveredDevices.update { services ->
                services.filter { it.name != serviceInfo.name } + serviceInfo
            }
        }
    }

    fun startDiscoveryAndPublish() {
        scope.launch {
            try {
                val localIp = getLocalIpAddress() ?: run {
                    Log.e(TAG, "无法获取本地IP地址")
                    return@launch
                }

                jmdns = withContext(Dispatchers.IO) {
                    JmDNS.create(localIp, JMDNS_NAME)
                }

                jmdns!!.apply {
                    addServiceListener(SERVICE_TYPE, serviceListener)
                    publishedService = ServiceInfo.create(
                        SERVICE_TYPE,
                        SERVICE_NAME,
                        SERVICE_PORT,
                        SERVICE_DESC
                    )
                    registerService(publishedService)
                }
                Log.d(TAG, "JmDNS 服务发现已启动")
                Log.d(TAG, "服务已发布: $SERVICE_NAME ($localIp:$SERVICE_PORT)")
            } catch (e: IOException) {
                Log.e(TAG, "启动JmDNS失败: ${e.message}")
            } catch (e: CancellationException) {
                Log.d(TAG, "协程已取消")
            } catch (e: Exception) {
                Log.e(TAG, "失败: ${e.message}")
            }
        }
    }

    fun stopDiscovery() {
        scope.launch {
            try {
                unpublishService()

                jmdns?.removeServiceListener(SERVICE_TYPE, serviceListener)
                withContext(Dispatchers.IO) {
                    jmdns?.close()
                }
                jmdns = null
                Log.d(TAG, "JmDNS 服务发现已停止")
            } catch (e: Exception) {
                Log.e(TAG, "停止JmDNS失败: ${e.message}")
            }
        }
    }

    private fun unpublishService() {
        scope.launch {
            try {
                publishedService?.let {
                    jmdns?.unregisterService(it)
                    publishedService = null
                    Log.d(TAG, "⏹️ 服务已取消发布: ${it.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "取消发布服务失败: ${e.message}")
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun getLocalIpAddress(): InetAddress? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null

        val linkProperties = cm.getLinkProperties(network)

        return linkProperties?.linkAddresses?.firstOrNull { address ->
            address.address is Inet4Address && !address.address.isLoopbackAddress
        }?.address
    }

    companion object {
        private const val TAG = "JmDNS"
        private const val JMDNS_NAME = "AndroidDevice"
        private const val SERVICE_TYPE = "_http._tcp.local."

        // 发布服务相关信息
        private const val SERVICE_DESC = "platform-android"
        private val SERVICE_NAME = "android-${Build.MODEL}-${Build.ID}"
        const val SERVICE_PORT = 8081
    }
}
