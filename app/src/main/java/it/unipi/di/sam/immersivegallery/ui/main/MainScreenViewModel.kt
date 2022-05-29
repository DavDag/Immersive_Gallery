package it.unipi.di.sam.immersivegallery.ui.main

import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.unipi.di.sam.immersivegallery.models.ALL_BUCKET_FILTER
import it.unipi.di.sam.immersivegallery.models.ImageData
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilterBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainScreenViewModel : ViewModel() {

    private val _filtersLoaded = SingleEventLiveData<Unit>()
    public val filtersLoaded: LiveData<Unit> = _filtersLoaded

    private val _images = SingleEventLiveData<List<ImageData>>()
    public val images: LiveData<List<ImageData>> = _images

    private val _buckets = SingleEventLiveData<List<ImageSearchFilterBucket>>()
    public val buckets: LiveData<List<ImageSearchFilterBucket>> = _buckets

    data class ImageSearchFilters(
        var bucket: ImageSearchFilterBucket,
    )

    private var _filters: ImageSearchFilters = ImageSearchFilters(
        bucket = ALL_BUCKET_FILTER
    )

    private val _reload = SingleEventLiveData<ImageSearchFilters>()
    public val reload: LiveData<ImageSearchFilters> = _reload

    fun loadFiltersQueryAsync(query: Cursor) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000L)
            query.use { cursor ->

                val map = mutableMapOf<Long, ImageSearchFilterBucket>(
                    ALL_BUCKET_FILTER.bucketId to ALL_BUCKET_FILTER
                )

                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    map[id] = ImageSearchFilterBucket(
                        bucketId = id,
                        bucketName = name,
                        displayName = name,
                    )
                }

                _buckets.postValue(map.values.toList())
                _filtersLoaded.postValue(Unit)
            }
        }
    }

    fun loadImagesQueryAsync(query: Cursor) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000L)
            query.use { cursor ->
                val list = mutableListOf<ImageData>()

                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val widthColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketIdColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getInt(sizeColumn)
                    val mime = cursor.getString(mimeColumn)
                    val bucketId = cursor.getLong(bucketIdColumn)
                    val bucketName = cursor.getString(bucketNameColumn)

                    list.add(
                        ImageData(
                            id = id,
                            width = width,
                            height = height,
                            size = size,
                            mime = mime,
                            bucketId = bucketId,
                            bucketName = bucketName,
                        )
                    )
                }

                _images.postValue(list)
            }
        }
    }

    // =======================================================

    fun restoreFilters(filters: ImageSearchFilters) {
        _filters = filters
        _reload.postValue(_filters)
    }

    fun updateSelectedImage(selectedPosition: Int) {
        // TODO
    }

    fun updateSelectedAlbum(selectedPosition: Int) {
        _filters.bucket = _buckets.value?.get(selectedPosition) ?: return
        _reload.postValue(_filters)
    }

}