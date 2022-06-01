package it.unipi.di.sam.immersivegallery.models

import android.net.Uri

data class ImageData(
    val uri: Uri,
    val id: Long,
    val width: Int,
    val height: Int,
    val size: Int,
    val mime: String,
    val dataTaken: Long,
    val dataModified: Long,
    val bucketId: Long,
    val bucketName: String,
)
