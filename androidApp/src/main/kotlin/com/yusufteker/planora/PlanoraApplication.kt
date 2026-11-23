package com.yusufteker.planora

import android.app.Application
import android.content.Context
import com.yusufteker.planora.android.widget.PlanoraWidgetUpdater
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.utils.NotificationSyncBridge
import com.yusufteker.planora.di.initKoin
import com.yusufteker.planora.feature.auth.presentation.onboarding.OnboardingViewModel
import com.yusufteker.planora.feature.home.data.repository.PlanRepositoryImpl
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.reminder.DailyDigestScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Planora Android Application class.
 *
 * Keeps Application.onCreate() minimal on the Main Thread:
 * - Registers lazy Koin modules only
 * - Reads instant cached startup route (< 1ms, no Keystore/DataStore stall)
 * - Verifies authoritative session/onboarding state & wires background services on IO
 */
class PlanoraApplication : Application(), KoinComponent {

    companion object {
        private const val STARTUP_CACHE_PREFS = "planora_startup_route_cache"
        private const val KEY_ROUTE = "initial_route"
        private const val ROUTE_ONBOARDING = "onboarding"
        private const val ROUTE_MAIN = "main"
        private const val ROUTE_LOGIN = "login"
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Resolves the first destination instantaneously from cache when available,
     * falling back to async IO resolution on first run.
     */
    val initialScreenDeferred: CompletableDeferred<Screen> = CompletableDeferred()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PlanoraApplication)
        }

        val startupPrefs = getSharedPreferences(STARTUP_CACHE_PREFS, Context.MODE_PRIVATE)
        val cachedRoute = when (startupPrefs.getString(KEY_ROUTE, null)) {
            ROUTE_ONBOARDING -> Screen.Onboarding
            ROUTE_MAIN -> Screen.Main()
            ROUTE_LOGIN -> Screen.Login
            else -> null
        }
        if (cachedRoute != null) {
            initialScreenDeferred.complete(cachedRoute)
        }

        // 1. Authoritative route resolution + continuous cache sync on IO
        appScope.launch {
            val sessionPreferences: SessionPreferences = get()
            val resolvedScreen = try {
                val snapshot = sessionPreferences.getStartupSessionSnapshot()
                val isLoggedIn = if (snapshot.isFirstRun) {
                    sessionPreferences.clearSession()
                    sessionPreferences.markAppAsRun()
                    false
                } else {
                    sessionPreferences.getRefreshToken() != null
                }
                when {
                    snapshot.lastSeenOverviewIndex < OnboardingViewModel.TOTAL_PAGES -> Screen.Onboarding
                    isLoggedIn -> Screen.Main()
                    else -> Screen.Login
                }
            } catch (_: Exception) {
                Screen.Login
            }
            persistRouteCache(startupPrefs, resolvedScreen)
            if (!initialScreenDeferred.isCompleted) {
                initialScreenDeferred.complete(resolvedScreen)
            }

            // Keep instant startup cache in sync whenever login/logout/onboarding changes
            launch {
                combine(
                    sessionPreferences.userIdFlow,
                    sessionPreferences.lastSeenOverviewIndexFlow
                ) { userId, lastSeen ->
                    when {
                        lastSeen < OnboardingViewModel.TOTAL_PAGES -> Screen.Onboarding
                        !userId.isNullOrBlank() -> Screen.Main()
                        else -> Screen.Login
                    }
                }.collect { screen ->
                    persistRouteCache(startupPrefs, screen)
                }
            }

            // 2. Non-critical background setup after startup route is resolved
            try {
                DailyDigestScheduler.scheduleAll(this@PlanoraApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                NotificationSyncBridge.onWidgetUpdateRequested = {
                    PlanoraWidgetUpdater.updateAllWidgets(this@PlanoraApplication)
                }
                val planRepository: PlanRepository = get()
                (planRepository as? PlanRepositoryImpl)?.setDataChangeListener {
                    PlanoraWidgetUpdater.updateAllWidgets(this@PlanoraApplication)
                }
                PlanoraWidgetUpdater.updateAllWidgets(this@PlanoraApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun persistRouteCache(prefs: android.content.SharedPreferences, screen: Screen) {
        val value = when (screen) {
            is Screen.Onboarding -> ROUTE_ONBOARDING
            is Screen.Main -> ROUTE_MAIN
            else -> ROUTE_LOGIN
        }
        if (prefs.getString(KEY_ROUTE, null) != value) {
            prefs.edit().putString(KEY_ROUTE, value).apply()
        }
    }
}
