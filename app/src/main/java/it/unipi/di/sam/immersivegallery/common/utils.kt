package it.unipi.di.sam.immersivegallery.common

fun <T> reservedMutableListOf(capacity: Int): MutableList<T> = ArrayList(capacity)
