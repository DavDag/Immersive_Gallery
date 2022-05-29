package it.unipi.di.sam.immersivegallery.common

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class SingleEventLiveData<T> : MediatorLiveData<T>() {

    private val _observers = mutableListOf<ObserverWrapper<in T>>()

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        ObserverWrapper(observer).let { wrapper ->
            _observers.add(wrapper)
            super.observe(owner, wrapper)
        }
    }

    @MainThread
    override fun removeObserver(observer: Observer<in T>) {
        _observers.removeIf { it.observer == observer }
        super.removeObserver(observer)
    }

    @MainThread
    override fun setValue(t: T?) {
        _observers.forEach { it.newValue() }
        super.setValue(t)
    }

    @MainThread
    override fun postValue(value: T) {
        _observers.forEach { it.newValue() }
        super.postValue(value)
    }

    private class ObserverWrapper<T>(val observer: Observer<T>) : Observer<T> {

        private var _pending = false

        override fun onChanged(t: T?) {
            if (_pending) {
                _pending = false
                observer.onChanged(t)
            }
        }

        fun newValue() {
            _pending = true
        }
    }
}