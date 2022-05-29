package it.unipi.di.sam.immersivegallery.models

data class ImageData(
    val id: Long,
    val width: Int,
    val height: Int,
    val size: Int,
    val mime: String,
    val bucketId: Long,
    val bucketName: String,
)
