package com.yusufteker.planora.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class AndroidExternalAppLauncher(private val context: Context) : ExternalAppLauncher {
    override fun isAppInstalled(app: ExternalApp): Boolean {
        return try {
            val pm = context.packageManager
            // 1. Check if launch intent exists by package name
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                return true
            }
            // 2. Check if deep link scheme resolves
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.scheme))
            intent.resolveActivity(pm) != null
        } catch (_: Exception) {
            false
        }
    }

    override fun openApp(app: ExternalApp): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.scheme)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (viewIntent.resolveActivity(pm) != null) {
                    context.startActivity(viewIntent)
                    true
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
actual fun rememberExternalAppLauncher(): ExternalAppLauncher {
    val context = LocalContext.current
    return remember(context) {
        AndroidExternalAppLauncher(context)
    }
}
