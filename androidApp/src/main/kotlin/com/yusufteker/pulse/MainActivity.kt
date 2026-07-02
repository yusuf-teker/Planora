package com.yusufteker.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import com.google.firebase.FirebaseApp
/**
 * Main activity for the Android app.
 *
 * Sets up edge-to-edge display and delegates to the shared [App] composable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)

        // Initialize Napier KMP logging (only in debug builds)
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        setContent {
            App()
        }
    }
}