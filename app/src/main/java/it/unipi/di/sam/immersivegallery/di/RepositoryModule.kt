package it.unipi.di.sam.immersivegallery.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.unipi.di.sam.immersivegallery.api.SharedPrefsRepository
import it.unipi.di.sam.immersivegallery.api.SharedPrefsRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    fun providesSharedPrefsRepository(impl: SharedPrefsRepositoryImpl): SharedPrefsRepository = impl

}