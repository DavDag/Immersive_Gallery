package it.unipi.di.sam.immersivegallery.models

import kotlinx.serialization.Serializable

@Serializable
data class ImageSearchFilterBucket(
    val bucketId: Long,
    val bucketName: String,
    val displayName: String,
)

@Serializable
data class ImageSearchFilters(
    var bucket: ImageSearchFilterBucket,
)

val ALL_BUCKET_FILTER = ImageSearchFilterBucket(
    bucketId = -1L,
    bucketName = "",
    displayName = "All"
)

val DEFAULT_FILTERS = ImageSearchFilters(
    bucket = ALL_BUCKET_FILTER
)