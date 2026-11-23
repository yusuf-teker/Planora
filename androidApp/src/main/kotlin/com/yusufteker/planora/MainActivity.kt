package com.yusufteker.planora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.yusufteker.planora.core.icon.AndroidAppIconManager
import com.yusufteker.planora.core.icon.AppIcon
import com.yusufteker.planora.core.navigation.DeepLinkManager
import com.yusufteker.planora.core.navigation.Screen
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

/**
 * Main activity for the Android app.
 *
 * Uses Android's native SplashScreen while the startup route resolves on IO,
 * then jumps straight into Main / Login / Onboarding with no secondary custom splash.
 */
class MainActivity : ComponentActivity() {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val currentIcon = AndroidAppIconManager(this).getCurrentIcon()
        if (currentIcon != AppIcon.DEFAULT) {
            val themeRes = when (currentIcon) {
                AppIcon.DEFAULT -> R.style.Theme_Planora
                AppIcon.PURPLE -> R.style.Theme_Planora_Purple
                AppIcon.GREEN -> R.style.Theme_Planora_Green
                AppIcon.ORANGE -> R.style.Theme_Planora_Orange
                AppIcon.PINK -> R.style.Theme_Planora_Pink
                AppIcon.RED -> R.style.Theme_Planora_Red
            }
            setTheme(themeRes)
        }

        val splashScreen = installSplashScreen()
        val planoraApp = application as? PlanoraApplication
        var resolvedInitialScreen by mutableStateOf<Screen?>(
            planoraApp?.initialScreenDeferred?.let { deferred ->
                if (deferred.isCompleted) runCatching { deferred.getCompleted() }.getOrNull() else null
            }
        )
        splashScreen.setKeepOnScreenCondition { planoraApp != null && resolvedInitialScreen == null }

        enableEdgeToEdge()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        super.onCreate(savedInstanceState)

        // Initialize Napier KMP logging (only in debug builds)
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        if (resolvedInitialScreen == null && planoraApp != null) {
            lifecycleScope.launch {
                resolvedInitialScreen = planoraApp.initialScreenDeferred.await()
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Napier.d("Notification permission granted", tag = "MainActivity")
                } else {
                    Napier.w("Notification permission denied", tag = "MainActivity")
                }
            }

            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Handle initial intent
        handleIntent(intent)

        setContent {
            if (planoraApp == null || resolvedInitialScreen != null) {
                App(initialScreen = resolvedInitialScreen)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val type = intent?.type
        val dataString = intent?.dataString
        val selectedDate = intent?.getStringExtra(com.yusufteker.planora.android.widget.PlanoraWidgetUpdater.EXTRA_SELECTED_DATE)
        val taskId = intent?.getStringExtra(com.yusufteker.planora.android.widget.PlanoraWidgetUpdater.EXTRA_TASK_ID)

        if (BuildConfig.DEBUG) {
            Napier.d("DEEPLINK DEBUG: MainActivity handleIntent action='$action', type='$type', dataString='$dataString', selectedDate='$selectedDate', taskId='$taskId'")
        }

        if (action == Intent.ACTION_SEND && type?.startsWith("text/") == true) {
            val title = intent.getStringExtra(Intent.EXTRA_TITLE)
                ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                ?: "Market Listesi"
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encodedNote = java.net.URLEncoder.encode(text, "UTF-8")
            DeepLinkManager.emitLink("planora://share/task?title=$encodedTitle&note=$encodedNote")
        } else if (selectedDate != null) {
            DeepLinkManager.emitLink("planora://share/calendar?date=$selectedDate")
        } else if (taskId != null) {
            DeepLinkManager.emitLink("planora://share/task?taskId=$taskId")
        } else if (dataString != null) {
            DeepLinkManager.emitLink(dataString)
        }
    }
}