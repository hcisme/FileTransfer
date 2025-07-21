package com.chc.filetransferandroid.ui.screen

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import okio.source
import org.json.JSONObject
import java.net.Inet4Address
import javax.jmdns.ServiceInfo

class UploadViewModel(application: Application) : AndroidViewModel(application) {
    val client by lazy { OkHttpClient() }
    var selectedService by mutableStateOf<ServiceInfo?>(null)
    val selectedFiles = mutableStateListOf<Uri>()
    var isUploading by mutableStateOf(false)
    var uploadProgress by mutableFloatStateOf(0f)

    fun upload() {
        if (selectedService == null) {
            showToast("请先选择服务")
            return
        }
        if (selectedFiles.isEmpty()) {
            showToast("请选择文件")
            return
        }
        isUploading = true

        uploadFiles(
            onProgress = { progress -> uploadProgress = "%.2f".format(progress).toFloat() },
            onComplete = { text ->
                isUploading = false
                uploadProgress = 0f
                selectedFiles.clear()
                showToast(text)
            }
        )
    }

    private fun uploadFiles(
        onProgress: (progress: Float) -> Unit,
        onComplete: (message: String) -> Unit
    ) {
        val service = selectedService!!
        val ip = service.inetAddresses.firstOrNull { it is Inet4Address }?.hostAddress ?: return
        val url = "http://$ip:${service.port}/upload"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val fileSizes = calcFileListSize()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .apply {
                        selectedFiles.forEachIndexed { index, uri ->
                            // 文件名
                            val cursor = resolver.query(
                                uri,
                                arrayOf(OpenableColumns.DISPLAY_NAME),
                                null, null, null
                            )
                            val fileName = cursor?.use {
                                if (it.moveToFirst()) it.getString(0) else "unknown"
                            } ?: "unknown"

                            // MIME 类型
                            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                            val mediaType = mimeType.toMediaTypeOrNull()

                            // 用 InputStream 流包裹 RequestBody
                            val streamBody = object : RequestBody() {
                                override fun contentType() = mediaType
                                override fun contentLength() = fileSizes[index]
                                override fun writeTo(sink: BufferedSink) {
                                    resolver.openInputStream(uri)?.use { input ->
                                        input.source().use { source ->
                                            sink.writeAll(source)
                                        }
                                    }
                                }
                            }

                            addFormDataPart("files", fileName, streamBody)
                        }
                    }
                    .build()

                val totalBytes = multipartBody.contentLength()

                val progressBody = object : RequestBody() {
                    override fun contentType() = multipartBody.contentType()
                    override fun contentLength() = totalBytes
                    override fun writeTo(sink: BufferedSink) {
                        val countingSink = object : ForwardingSink(sink) {
                            var bytesWritten = 0L

                            override fun write(source: Buffer, byteCount: Long) {
                                super.write(source, byteCount)
                                bytesWritten += byteCount
                                onProgress(if (totalBytes > 0) (bytesWritten.toFloat() * 100 / totalBytes) else 0f)
                            }
                        }

                        // 写入实际内容
                        val buffered = countingSink.buffer()
                        multipartBody.writeTo(buffered)
                        buffered.flush()
                    }
                }

                val request = Request.Builder().url(url).post(progressBody).build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string()
                    if (response.isSuccessful && bodyStr != null) {
                        val json = JSONObject(bodyStr)
                        if (json.optInt("code") == 200) {
                            onComplete("上传成功")
                        } else {
                            onComplete("服务错误: ${json.optString("message")}")
                        }
                    } else {
                        onComplete("HTTP错误: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("UploadViewModel", "upload failed", e)
                onComplete("上传失败: ${e.message ?: "未知错误"}")
            }
        }
    }


    private fun showToast(message: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calcFileListSize(): List<Long> {
        val resolver = getApplication<Application>().contentResolver

        return selectedFiles.map { uri ->
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) cursor.getLong(sizeIndex) else -1L
                } else -1L
            } ?: -1L
        }
    }
}
