package com.yusufteker.pulse

import android.app.Application
import com.yusufteker.pulse.di.initKoin
import org.koin.android.ext.koin.androidContext

/**
 * Pulsy Android Application class.
 *
 * Initializes Koin with Android context at application startup.
 */
class PulsyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PulsyApplication)
        }
    }
}
