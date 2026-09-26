package com.yusufteker.planora.core.calendar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Android actual implementation of [rememberCalendarPermissionLauncher].
 *
 * Checks if `android.permission.READ_CALENDAR` is already granted.
 * If not, delegates to the Android Activity Result API.
 */
@Composable
actual fun rememberCalendarPermissionLauncher(
    onResult: (Boolean) -> Unit
): CalendarPermissionLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult
    )

    return remember(launcher, context) {
        object : CalendarPermissionLauncher {
            override fun launch() {
                val isGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED

                if (isGranted) {
                    onResult(true)
                } else {
                    launcher.launch(Manifest.permission.READ_CALENDAR)
                }
            }
        }
    }
}
