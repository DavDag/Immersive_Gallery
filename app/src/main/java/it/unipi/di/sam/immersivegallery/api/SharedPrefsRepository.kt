package it.unipi.di.sam.immersivegallery.api

import android.content.SharedPreferences
import dagger.Lazy
import it.unipi.di.sam.immersivegallery.models.ImageSearchFilters
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface SharedPrefsRepository {
    public suspend fun loadSavedFilters(): ImageSearchFilters?
    public suspend fun saveFilters(filters: ImageSearchFilters)
}

@Singleton
class SharedPrefsRepositoryImpl @Inject constructor(
    private val _sharedPreferences: Lazy<SharedPreferences>
) : SharedPrefsRepository {

    companion object {
        const val SEARCH_FILTERS_KEY = "search_filters_key"
    }

    override suspend fun loadSavedFilters(): ImageSearchFilters? =
        _sharedPreferences.get().getString(SEARCH_FILTERS_KEY, null)
            ?.run(Json::decodeFromString)

    override suspend fun saveFilters(filters: ImageSearchFilters): Unit =
        with(_sharedPreferences.get().edit()) {
            putString(SEARCH_FILTERS_KEY, Json.encodeToString(filters))
            apply()
        }

}
