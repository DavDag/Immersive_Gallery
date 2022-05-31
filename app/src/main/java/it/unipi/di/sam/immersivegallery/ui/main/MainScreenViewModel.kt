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
import kotlinx.coroutines.delay
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

    public var hasLoadedFilters = false
    public var filters = DEFAULT_FILTERS
    private val _reload = SingleEventLiveData<Unit>()
    public val reload: LiveData<Unit> = _reload

    // =======================================================

    fun loadFiltersQueryAsync(query: Cursor) {
        hasLoadedFilters = false

        viewModelScope.launch(Dispatchers.IO) {
            delay(2000L)
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

                _filtersDataLoaded.postValue(
                    ImageSearchFiltersData(
                        buckets = map.values.toList()
                    )
                )
            }

            hasLoadedFilters = true
        }
    }

    // =======================================================

    fun getOldFilters() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = sharedPrefsRepository.loadSavedFilters() ?: DEFAULT_FILTERS
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

}