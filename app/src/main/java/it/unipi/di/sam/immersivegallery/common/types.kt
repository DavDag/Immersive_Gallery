package it.unipi.di.sam.immersivegallery.common

import android.view.LayoutInflater
import android.view.ViewGroup

typealias InflateFun<T> = (LayoutInflater, ViewGroup?, Boolean) -> T
