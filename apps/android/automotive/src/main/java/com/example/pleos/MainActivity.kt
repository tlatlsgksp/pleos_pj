package com.example.pleos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pleos.service.VehicleRecordingService
import com.example.pleos.ui.navigation.PleosNavHost
import com.example.pleos.ui.theme.PleosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Started here, while in the foreground, so recording survives other apps covering this one.
        VehicleRecordingService.start(this)
        setContent {
            PleosTheme {
                PleosNavHost()
            }
        }
    }
}
