package it.unipi.di.sam.immersivegallery.models

import kotlinx.serialization.Serializable

@Serializable
data class ImageSearchFilterBucket(
    val bucketId: Long,
    val bucketName: String,
    val displayName: String,
)

@Serializable
data class ImageSearchFilterSize(
    val size: Int,
    val displayName: String,
)

@Serializable
data class ImageSearchFilterMime(
    val mime: String,
    val displayName: String,
)

@Serializable
data class ImageSearchFilters(
    var bucket: ImageSearchFilterBucket,
    var sizeMin: ImageSearchFilterSize,
    var sizeMax: ImageSearchFilterSize,
    var mime: ImageSearchFilterMime,
)

data class ImageSearchFiltersData(
    val buckets: List<ImageSearchFilterBucket>,
    val sizes: List<ImageSearchFilterSize>,
    val mimes: List<ImageSearchFilterMime>,
)

val ALL_BUCKET_FILTER = ImageSearchFilterBucket(
    bucketId = -1L,
    bucketName = "",
    displayName = "All"
)

val ZERO_SIZE_FILTER = ImageSearchFilterSize(
    size = 0,
    displayName = "Zero",
)

val INF_SIZE_FILTER = ImageSearchFilterSize(
    size = Int.MAX_VALUE,
    displayName = "Inf",
)

val ALL_MIME_FILTER = ImageSearchFilterMime(
    mime = "images/*",
    displayName = "All",
)

val DEFAULT_FILTERS = ImageSearchFilters(
    bucket = ALL_BUCKET_FILTER,
    sizeMin = ZERO_SIZE_FILTER,
    sizeMax = INF_SIZE_FILTER,
    mime = ALL_MIME_FILTER,
)
