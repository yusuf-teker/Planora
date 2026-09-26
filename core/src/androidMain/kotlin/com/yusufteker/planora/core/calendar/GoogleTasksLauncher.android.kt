package com.yusufteker.planora.core.calendar

import android.accounts.Account
import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GoogleTasks"

/**
 * Android implementation of [rememberGoogleTasksLauncher].
 *
 * Utilizes Google Play Services Auth to request user consent for `tasks.readonly`,
 * exchanges the credential for an OAuth 2.0 access token via [GoogleAuthUtil],
 * and fetches the user's Google Tasks via the Google Tasks REST API.
 */
@Composable
actual fun rememberGoogleTasksLauncher(
    onResult: (List<CalendarImportItem>?, String?) -> Unit
): GoogleTasksLauncher {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pendingAccount by remember { mutableStateOf<Account?>(null) }
    var pendingEmail by remember { mutableStateOf<String?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { consentResult ->
        Log.d(TAG, "ConsentActivity result: resultCode=${consentResult.resultCode}")
        val account = pendingAccount
        val email = pendingEmail ?: "Google Tasks"
        if (consentResult.resultCode == Activity.RESULT_OK && account != null) {
            coroutineScope.launch {
                try {
                    Log.d(TAG, "Consent granted by user. Retrying token retrieval for $email...")
                    val scope = "oauth2:https://www.googleapis.com/auth/tasks.readonly https://www.googleapis.com/auth/calendar.events.readonly"
                    val accessToken = withContext(Dispatchers.IO) {
                        GoogleAuthUtil.getToken(context, account, scope)
                    }
                    Log.d(TAG, "Access token received after consent. Fetching tasks and calendar events...")
                    val items = withContext(Dispatchers.IO) {
                        GoogleTasksClient.fetchAllGoogleData(accessToken, email)
                    }
                    Log.d(TAG, "Successfully fetched ${items.size} items after consent.")
                    onResult(items, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching items after consent: ${e.message}", e)
                    onResult(null, e.localizedMessage ?: "Google verileri alınamadı")
                }
            }
        } else {
            Log.w(TAG, "Consent cancelled or denied (resultCode=${consentResult.resultCode})")
            onResult(null, "Google erişim izni onaylanmadı.")
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "SignIn launcher returned: resultCode=${result.resultCode}, hasData=${result.data != null}")
        if (result.data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val email = account.email ?: "Google Tasks"
                val androidAccount = account.account ?: Account(email, "com.google")
                pendingAccount = androidAccount
                pendingEmail = email
                Log.d(TAG, "Google Account selected: $email (displayName=${account.displayName})")

                val scope = "oauth2:https://www.googleapis.com/auth/tasks.readonly https://www.googleapis.com/auth/calendar.events.readonly"
                coroutineScope.launch {
                    try {
                        Log.d(TAG, "Requesting token with scope=$scope...")
                        val accessToken = withContext(Dispatchers.IO) {
                            GoogleAuthUtil.getToken(context, androidAccount, scope)
                        }
                        Log.d(TAG, "OAuth access token received successfully. Fetching tasks and calendar events...")
                        val items = withContext(Dispatchers.IO) {
                            GoogleTasksClient.fetchAllGoogleData(accessToken, email)
                        }
                        Log.d(TAG, "Fetched ${items.size} items from Google APIs.")
                        onResult(items, null)
                    } catch (e: UserRecoverableAuthException) {
                        Log.w(TAG, "UserRecoverableAuthException: consent dialog required. Opening consent screen...", e)
                        val consentIntent = e.intent
                        if (consentIntent != null) {
                            consentLauncher.launch(consentIntent)
                        } else {
                            onResult(null, "Google Takvim ve Görevler için kullanıcı onayı gerekiyor.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting token or fetching items: ${e.message}", e)
                        onResult(null, e.localizedMessage ?: "Google verileri alınamadı")
                    }
                }
            } catch (e: ApiException) {
                val code = e.statusCode
                val desc = CommonStatusCodes.getStatusCodeString(code)
                Log.e(TAG, "GoogleSignIn ApiException: statusCode=$code ($desc), msg=${e.message}", e)
                if (code == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    onResult(null, "Google hesap seçimi iptal edildi.")
                } else if (code == 10) {
                    Log.e(TAG, "DEVELOPER_ERROR (10): Keystore SHA-1 (35:B4:9F:BA:D3:A5:50:7B:B1:4D:07:63:4F:FB:45:F9:FC:19:48:1A) and package com.yusufteker.planora must be added to Google Cloud Console OAuth 2.0 Client IDs.")
                    onResult(null, "Google Cloud OAuth SHA-1 hatası (DEVELOPER_ERROR 10). Google Cloud Console'a SHA-1 eklenmelidir.")
                } else {
                    onResult(null, "Google giriş hatası: $desc ($code)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error reading GoogleSignIn result: ${e.message}", e)
                onResult(null, e.localizedMessage)
            }
        } else {
            Log.w(TAG, "SignIn launcher returned null data. resultCode=${result.resultCode}")
            onResult(null, "Google hesap seçimi yapılamadı (kod: ${result.resultCode})")
        }
    }

    return remember(launcher, consentLauncher, context) {
        object : GoogleTasksLauncher {
            override fun launch() {
                try {
                    Log.d(TAG, "Launching Google Sign-In with tasks.readonly and calendar.events.readonly scopes...")
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(
                            Scope("https://www.googleapis.com/auth/tasks.readonly"),
                            Scope("https://www.googleapis.com/auth/calendar.events.readonly")
                        )
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    launcher.launch(client.signInIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error initiating Google Sign-In client: ${e.message}", e)
                    onResult(null, e.localizedMessage)
                }
            }
        }
    }
}
