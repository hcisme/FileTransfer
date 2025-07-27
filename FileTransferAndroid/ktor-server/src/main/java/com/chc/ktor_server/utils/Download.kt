package com.chc.ktor_server.utils

import android.content.Context
import android.util.Log
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.PublicDirectory
import io.ktor.http.content.PartData
import io.ktor.utils.io.jvm.javaio.toInputStream

fun saveToPublicDownloads(
    context: Context,
    fileName: String,
    part: PartData.FileItem
) {
    try {
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val targetDirectory = when (extension) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp" -> PublicDirectory.PICTURES
            "mp3", "wav", "flac", "ogg", "m4a" -> PublicDirectory.MUSIC
            "mp4", "mkv", "avi", "mov", "flv" -> PublicDirectory.MOVIES
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt" -> PublicDirectory.DOCUMENTS
            "apk" -> PublicDirectory.DOWNLOADS
            else -> PublicDirectory.DOWNLOADS
        }

        val downloadsDir = DocumentFileCompat.fromPublicFolder(context, targetDirectory)
            ?: throw Exception("Downloads directory not found")

        val appSubDir = downloadsDir.findFile(context.packageName)
            ?: downloadsDir.createDirectory(context.packageName)
            ?: downloadsDir

        val file = appSubDir.createFile(getMimeType(fileName), fileName)
            ?: throw Exception("Failed to create file: $fileName")

        context.contentResolver.openOutputStream(file.uri)?.use { output ->
            part.provider().toInputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw Exception("Failed to open output stream")
    } catch (e: Exception) {
        Log.e("Save_File", "Failed to save file: $fileName", e)
    }
}

private fun getMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "ogg" -> "audio/ogg"
        "m4a" -> "audio/mp4"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "flv" -> "video/x-flv"
        "zip" -> "application/zip"
        "apk" -> "application/vnd.android.package-archive"
        else -> "application/octet-stream"
    }
}
