package com.yusufteker.planora

import android.app.Application
import com.yusufteker.planora.di.initKoin
import com.yusufteker.planora.reminder.DailyDigestScheduler
import org.koin.android.ext.koin.androidContext

/**
 * Planora Android Application class.
 *
 * Initializes Koin with Android context at application startup.
 */
class PlanoraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PlanoraApplication)
        }
        DailyDigestScheduler.scheduleAll(this)
    }
}
