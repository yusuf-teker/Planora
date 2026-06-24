package com.yusufteker.pulse

import android.app.Application
import com.yusufteker.pulse.di.initKoin
import org.koin.android.ext.koin.androidContext

/**
 * Pulse Android Application class.
 *
 * Initializes Koin with Android context at application startup.
 */
class PulseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PulseApplication)
        }
    }
}
