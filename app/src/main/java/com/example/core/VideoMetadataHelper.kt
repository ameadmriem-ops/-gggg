package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class LocalVideoFileInfo(
    val uri: Uri,
    val fileName: String,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val durationSeconds: Int,
    val durationFormatted: String,
    val width: Int,
    val height: Int,
    val resolutionLabel: String,
    val isPortrait: Boolean,
    val thumbnailUri: String,
    val mimeType: String?
)

object VideoMetadataHelper {

    suspend fun extractMetadata(context: Context, videoUri: Uri): LocalVideoFileInfo = withContext(Dispatchers.IO) {
        var fileName = "video_${System.currentTimeMillis()}"
        var fileSizeBytes: Long = 0L

        // 1. Query ContentResolver for file name & size
        try {
            context.contentResolver.query(videoUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { fileName = it }
                    }
                    if (sizeIndex != -1) {
                        fileSizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        if (fileName.isBlank() || fileName.startsWith("content:")) {
            fileName = videoUri.lastPathSegment ?: "device_video.mp4"
        }

        // Human-readable size
        val fileSizeFormatted = when {
            fileSizeBytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(fileSizeBytes / (1024.0 * 1024.0 * 1024.0))
            fileSizeBytes >= 1024 * 1024 -> "%.1f MB".format(fileSizeBytes / (1024.0 * 1024.0))
            fileSizeBytes >= 1024 -> "%.1f KB".format(fileSizeBytes / 1024.0)
            fileSizeBytes > 0 -> "$fileSizeBytes B"
            else -> "حجم تلقائي"
        }

        // 2. MediaMetadataRetriever for duration, dimensions, rotation, thumbnail
        var durationMs = 0L
        var width = 1280
        var height = 720
        var mimeType: String? = null
        var thumbPath = ""

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durStr?.toLongOrNull() ?: 0L

            val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            val rawW = wStr?.toIntOrNull() ?: 1280
            val rawH = hStr?.toIntOrNull() ?: 720
            val rotation = rotationStr?.toIntOrNull() ?: 0

            if (rotation == 90 || rotation == 270) {
                width = rawH
                height = rawW
            } else {
                width = rawW
                height = rawH
            }

            // Extract frame at 1s or start
            val frameTimeUs = if (durationMs > 2000) 1_000_000L else 0L
            val bitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (bitmap != null) {
                val thumbsDir = File(context.filesDir, "video_thumbs").apply { mkdirs() }
                val thumbFile = File(thumbsDir, "thumb_${UUID.randomUUID().toString().take(8)}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbPath = Uri.fromFile(thumbFile).toString()
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        val durationSeconds = ((durationMs + 500) / 1000).toInt().coerceAtLeast(1)
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        val durationFormatted = "%02d:%02d".format(minutes, seconds)

        val isPortrait = height > width
        val resolutionLabel = when {
            width >= 3840 || height >= 3840 -> "4K UHD"
            width >= 1920 || height >= 1920 -> "1080p FHD"
            width >= 1280 || height >= 1280 -> "720p HD"
            width >= 854 || height >= 854 -> "480p SD"
            else -> "${width}x${height}"
        }

        LocalVideoFileInfo(
            uri = videoUri,
            fileName = fileName,
            fileSizeFormatted = fileSizeFormatted,
            fileSizeBytes = fileSizeBytes,
            durationSeconds = durationSeconds,
            durationFormatted = durationFormatted,
            width = width,
            height = height,
            resolutionLabel = resolutionLabel,
            isPortrait = isPortrait,
            thumbnailUri = thumbPath,
            mimeType = mimeType
        )
    }
}
