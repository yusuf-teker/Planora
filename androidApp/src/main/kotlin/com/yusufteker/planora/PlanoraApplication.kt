package com.yusufteker.planora

import android.app.Application
import com.yusufteker.planora.android.widget.PlanoraWidgetUpdater
import com.yusufteker.planora.core.utils.NotificationSyncBridge
import com.yusufteker.planora.di.initKoin
import com.yusufteker.planora.feature.home.data.repository.PlanRepositoryImpl
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.reminder.DailyDigestScheduler
import org.koin.android.ext.koin.androidContext

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Planora Android Application class.
 *
 * Initializes Koin with Android context at application startup.
 */
class PlanoraApplication : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PlanoraApplication)
        }
        DailyDigestScheduler.scheduleAll(this)

        try {
            NotificationSyncBridge.onWidgetUpdateRequested = {
                PlanoraWidgetUpdater.updateAllWidgets(this@PlanoraApplication)
            }
            val planRepository: PlanRepository by inject()
            (planRepository as? PlanRepositoryImpl)?.setDataChangeListener {
                PlanoraWidgetUpdater.updateAllWidgets(this@PlanoraApplication)
            }
            PlanoraWidgetUpdater.updateAllWidgets(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
