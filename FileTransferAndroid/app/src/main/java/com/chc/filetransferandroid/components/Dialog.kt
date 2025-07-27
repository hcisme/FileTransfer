package com.chc.filetransferandroid.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dialog(
    modifier: Modifier = Modifier,
    visible: Boolean = false,
    properties: DialogProperties = DialogProperties(),
    confirmButtonText: String? = null,
    cancelButtonText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (visible) {
        BasicAlertDialog(
            modifier = modifier,
            properties = properties,
            onDismissRequest = {
                onDismissRequest?.invoke()
            }
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.small,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        icon?.invoke()
                        title?.invoke()

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    content()

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (cancelButtonText != null) {
                            TextButton(
                                onClick = {
                                    onDismissRequest?.invoke()
                                },
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(cancelButtonText)
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (confirmButtonText != null) {
                            TextButton(
                                onClick = {
                                    onConfirm?.invoke()
                                },
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(confirmButtonText)
                            }
                        }
                    }
                }
            }
        }
    }
}
