package com.yusufteker.planora.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yusufteker.planora.admin.ui.AdminMainScreen
import com.yusufteker.planora.admin.ui.AdminViewModel

/**
 * Planora Admin Uygulaması Başlangıç Aktivitesi.
 */
class PlanoraAdminActivity : ComponentActivity() {

    private val viewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AdminMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
