package com.tapago.feature.tracking.data

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.tapago.feature.tracking.domain.RunSessionRepository
import com.tapago.feature.tracking.domain.RunTrackingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingBindsModule {
    @Binds
    @Singleton
    abstract fun bindRunTrackingRepository(
        impl: LocationRunTrackingRepository,
    ): RunTrackingRepository

    @Binds
    @Singleton
    abstract fun bindRunSessionRepository(
        impl: InMemoryRunSessionRepository,
    ): RunSessionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object TrackingProvidesModule {
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context,
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
}
