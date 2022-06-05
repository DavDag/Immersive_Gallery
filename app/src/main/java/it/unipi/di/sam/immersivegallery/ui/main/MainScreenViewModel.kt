package it.unipi.di.sam.immersivegallery.ui.main

import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.unipi.di.sam.immersivegallery.api.SharedPrefsRepository
import it.unipi.di.sam.immersivegallery.common.MultipleEventLiveData
import it.unipi.di.sam.immersivegallery.common.SingleEventLiveData
import it.unipi.di.sam.immersivegallery.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val sharedPrefsRepository: SharedPrefsRepository
) : ViewModel() {

    private val _filtersDataLoaded = SingleEventLiveData<ImageSearchFiltersData>()
    private val _oldFiltersLoaded = SingleEventLiveData<ImageSearchFilters>()
    public val filtersData =
        MultipleEventLiveData
            .From(_filtersDataLoaded, _oldFiltersLoaded)
            .asLiveData()

    public var hasLoadedFiltersData = false
    public var hasLoadedOldFilters = false
    public var filters = DEFAULT_FILTERS
    private val _reload = SingleEventLiveData<Unit>()
    public val reload: LiveData<Unit> = _reload

    // =======================================================

    fun loadFiltersQueryAsync(query: Cursor) {
        hasLoadedFiltersData = false

        viewModelScope.launch(Dispatchers.Main) {
            // delay(10000L)
            query.use { cursor ->

                val bucketsMap = mutableMapOf(
                    ALL_BUCKET_FILTER.bucketId to ALL_BUCKET_FILTER
                )

                val sizes = mutableListOf(
                    ZERO_SIZE_FILTER,
                    KB_100_SIZE_FILTER,
                    MB_1_SIZE_FILTER,
                    MB_5_SIZE_FILTER,
                    MB_10_SIZE_FILTER,
                    INF_SIZE_FILTER,
                )

                val mimesMap = mutableMapOf(
                    ALL_MIME_FILTER.type to ALL_MIME_FILTER,
                )

                val sizeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketIdColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val size = cursor.getInt(sizeColumn)
                    val mime = cursor.getString(mimeColumn)
                    val bucketId = cursor.getLong(bucketIdColumn)
                    val bucketName = cursor.getString(bucketNameColumn)

                    bucketsMap[bucketId] = ImageSearchFilterBucket(
                        bucketId = bucketId,
                        bucketName = bucketName,
                        displayName = bucketName,
                    )

                    mimesMap[mime] = ImageSearchFilterMime(
                        type = mime,
                        displayName = mime,
                    )
                }

                hasLoadedFiltersData = true

                _filtersDataLoaded.postValue(
                    ImageSearchFiltersData(
                        buckets = bucketsMap.values.toList(),
                        sizes = sizes.toList(),
                        mimes = mimesMap.values.toList(),
                    )
                )
            }
        }
    }

    // =======================================================

    fun getOldFilters() {
        hasLoadedOldFilters = false
        viewModelScope.launch(Dispatchers.IO) {
            // delay(5000L)
            val res = sharedPrefsRepository.loadSavedFilters() ?: DEFAULT_FILTERS
            filters = res
            hasLoadedOldFilters = true
            _oldFiltersLoaded.postValue(res)
        }
    }

    fun saveFilters() {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPrefsRepository.saveFilters(filters)
        }
    }

    fun restoreFilters(filters: ImageSearchFilters) {
        this.filters = filters
        _reload.postValue(Unit)
    }

    fun updateSelectedAlbum(selectedPosition: Int) {
        filters.bucket = _filtersDataLoaded.value?.buckets?.get(selectedPosition) ?: return
        _reload.postValue(Unit)
    }

    fun updateSelectedSizeMin(selectedPosition: Int) {
        filters.sizeMin = _filtersDataLoaded.value?.sizes?.get(selectedPosition) ?: return
        _reload.postValue(Unit)
    }

    fun updateSelectedSizeMax(selectedPosition: Int) {
        filters.sizeMax = _filtersDataLoaded.value?.sizes?.get(selectedPosition) ?: return
        _reload.postValue(Unit)
    }

    fun updateSelectedMime(selectedPosition: Int) {
        filters.mime = _filtersDataLoaded.value?.mimes?.get(selectedPosition) ?: return
        _reload.postValue(Unit)
    }

}