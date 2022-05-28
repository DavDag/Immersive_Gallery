package it.unipi.di.sam.immersivegallery.ui.main

import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilterBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainScreenViewModel : ViewModel() {

    private val _reload = MutableLiveData<Unit>()
    public val reload: LiveData<Unit> = _reload

    private val _loading = MutableLiveData<Boolean>()
    public val loading: LiveData<Boolean> = _loading

    private val _buckets = MutableLiveData<List<ImageSearchFilterBucket>>()
    public val buckets: LiveData<List<ImageSearchFilterBucket>> = _buckets

    init {
        _loading.postValue(true)
    }

    fun loadFiltersQueryAsync(query: Cursor) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)

            val map = mutableMapOf<Long, ImageSearchFilterBucket>()
            query.use { cursor ->
                // Log.d("CC", cursor.count.toString())
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
                    )
                }
            }

            _buckets.postValue(map.values.toList())
            _loading.postValue(false)

            _reload.postValue(Unit)
        }
    }

}