package com.yusufteker.planora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import com.google.firebase.FirebaseApp
import android.content.Intent
import com.yusufteker.planora.core.navigation.DeepLinkManager

/**
 * Main activity for the Android app.
 *
 * Sets up edge-to-edge display and delegates to the shared [App] composable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val currentIcon = com.yusufteker.planora.core.icon.AndroidAppIconManager(this).getCurrentIcon()
        val themeRes = when (currentIcon) {
            com.yusufteker.planora.core.icon.AppIcon.DEFAULT -> R.style.Theme_Planora
            com.yusufteker.planora.core.icon.AppIcon.PURPLE -> R.style.Theme_Planora_Purple
            com.yusufteker.planora.core.icon.AppIcon.GREEN -> R.style.Theme_Planora_Green
            com.yusufteker.planora.core.icon.AppIcon.ORANGE -> R.style.Theme_Planora_Orange
            com.yusufteker.planora.core.icon.AppIcon.PINK -> R.style.Theme_Planora_Pink
            com.yusufteker.planora.core.icon.AppIcon.RED -> R.style.Theme_Planora_Red
        }
        setTheme(themeRes)
        installSplashScreen()
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)

        // Initialize Napier KMP logging (only in debug builds)
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
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
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val dataString = intent?.dataString
        val selectedDate = intent?.getStringExtra(com.yusufteker.planora.android.widget.PlanoraWidgetUpdater.EXTRA_SELECTED_DATE)
        val taskId = intent?.getStringExtra(com.yusufteker.planora.android.widget.PlanoraWidgetUpdater.EXTRA_TASK_ID)

        println("DEEPLINK DEBUG: MainActivity handleIntent dataString='$dataString', selectedDate='$selectedDate', taskId='$taskId'")

        if (selectedDate != null) {
            DeepLinkManager.emitLink("planora://share/calendar?date=$selectedDate")
        } else if (taskId != null) {
            DeepLinkManager.emitLink("planora://share/task?taskId=$taskId")
        } else if (dataString != null) {
            DeepLinkManager.emitLink(dataString)
        }
    }
}