package com.giorgospapapetrou.flightfinder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import timber.log.Timber

@HiltAndroidApp
class FlightFinderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // MapLibre needs to be initialized before any MapView is created.
        // The empty API key is fine because we use OpenStreetMap tiles directly.
        MapLibre.getInstance(this)
    }
}