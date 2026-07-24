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
        println("DEEPLINK DEBUG: MainActivity handleIntent dataString='$dataString'")
        if (dataString != null) {
            DeepLinkManager.emitLink(dataString)
        }
    }
}