package it.unipi.di.sam.immersivegallery

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Singleton

@HiltAndroidApp
@Singleton
class MyApp : Application() {}
