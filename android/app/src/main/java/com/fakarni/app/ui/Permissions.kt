package com.fakarni.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.fakarni.app.geo.GeofenceManager

/**
 * Android asks in two steps:
 * 1. "While using the app" location (+ notifications on Android 13+).
 * 2. "Allow all the time" location, which Android 11+ only offers on a settings screen.
 * Step 2 is what lets reminders work while the app is closed.
 */
class PermissionState(
    val foreground: Boolean,
    val background: Boolean,
    val requestForeground: () -> Unit,
    val requestBackground: () -> Unit,
)

@Composable
fun rememberPermissionState(geofences: GeofenceManager, onChanged: () -> Unit): PermissionState {
    var tick by remember { mutableIntStateOf(0) }
    val refresh = { tick++; onChanged() }

    val fgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    // Re-check when the user comes back from the settings screen.
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }

    val fgPerms = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    LaunchedEffect(Unit) { if (!geofences.hasForegroundLocation()) fgLauncher.launch(fgPerms) }

    val foreground = remember(tick) { geofences.hasForegroundLocation() }
    val background = remember(tick) { geofences.hasBackgroundLocation() }
    return PermissionState(
        foreground = foreground,
        background = background,
        requestForeground = { fgLauncher.launch(fgPerms) },
        requestBackground = {
            if (Build.VERSION.SDK_INT >= 29) bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        },
    )
}
