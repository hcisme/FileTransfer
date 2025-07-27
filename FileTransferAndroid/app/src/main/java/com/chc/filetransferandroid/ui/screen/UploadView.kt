package com.chc.filetransferandroid.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chc.filetransferandroid.components.Dialog
import com.chc.filetransferandroid.utils.JmDNSDeviceDiscovery
import com.chc.filetransferandroid.utils.LocalWsClient
import com.chc.filetransferandroid.utils.LocalWsServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadView(jm: JmDNSDeviceDiscovery, modifier: Modifier = Modifier) {
    val uploadViewModel = viewModel<UploadViewModel>()
    LocalWsServer.current.apply { setRequestListener(uploadViewModel) }
    val wsClient = LocalWsClient.current.apply { setResponseListener(uploadViewModel) }
    val deviceList by jm.discoveredDevices.collectAsState()
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            uploadViewModel.apply {
                selectedFiles.addAll(uris)
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text("文件传输")
            },
            actions = {
                IconButton(
                    onClick = {
                        filePicker.launch(arrayOf("*/*"))
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "已选文件: ${uploadViewModel.selectedFiles.size}",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = {
                    uploadViewModel.requestIsAllowUpload(wsClient)
                },
                shape = MaterialTheme.shapes.extraSmall,
                enabled = !uploadViewModel.isUploading,
            ) {
                if (uploadViewModel.isUploading) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("上传中")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${uploadViewModel.uploadProgress}%",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                } else {
                    Text("上传")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(deviceList) { service ->
                ServiceItem(
                    service = service,
                    isSelected = service.name == uploadViewModel.selectedService?.name,
                    onSelect = {
                        if (service.name == uploadViewModel.selectedService?.name) {
                            uploadViewModel.selectedService = null
                            return@ServiceItem
                        }
                        if (service.inetAddresses.firstOrNull()?.hostAddress == null) {
                            return@ServiceItem
                        }
                        uploadViewModel.selectedService = service
                    }
                )
            }
        }
    }

    Dialog(
        visible = uploadViewModel.isAllowTransfer,
        confirmButtonText = "确定",
        cancelButtonText = "取消",
        onConfirm = {
            uploadViewModel.isAllowTransfer = false
            // http 发送到 本地服务
            uploadViewModel.agreeRequest(true)
            uploadViewModel.showToast("已同意传输")
        },
        onDismissRequest = {
            uploadViewModel.isAllowTransfer = false
            uploadViewModel.agreeRequest(false)
            uploadViewModel.showToast("已拒绝传输")
        }
    ) {
        Text("是否接受来自（${uploadViewModel.requestId}）的文件传输")
    }
}
