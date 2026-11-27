package com.fakarni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.fakarni.app.ui.MapScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FakarniApp
        setContent {
            MaterialTheme {
                MapScreen(store = app.store, geofences = app.geofences)
            }
        }
    }
}
