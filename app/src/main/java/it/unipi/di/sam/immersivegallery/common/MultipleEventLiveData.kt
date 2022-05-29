package it.unipi.di.sam.immersivegallery.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class MultipleEventLiveData<T> constructor(
    private val generator: ((List<Any?>) -> T)
) : MediatorLiveData<T>() {

    private val _dataCache = mutableListOf<Any?>()

    private fun checkForAll() {
        val completed = (_dataCache.filterNotNull().size == _dataCache.size)
        if (completed) {
            value = generator(_dataCache)
        }
    }

    fun <K> addLiveData(liveData: LiveData<K>) {
        val index = _dataCache.size
        _dataCache.add(null)
        addSource(liveData) {
            _dataCache[index] = it
            checkForAll()
        }
    }

    fun asLiveData(): LiveData<T> = this

    companion object {

        private fun <T1, T2> ListToPair(list: List<Any?>) = Pair(list[0] as T1, list[1] as T2)

        fun <T1, T2> From(
            liveData1: LiveData<T1>,
            liveData2: LiveData<T2>
        ): MultipleEventLiveData<Pair<T1, T2>> =
            MultipleEventLiveData<Pair<T1, T2>>(::ListToPair)
                .also {
                    it.addLiveData(liveData1)
                    it.addLiveData(liveData2)
                }

    }

}