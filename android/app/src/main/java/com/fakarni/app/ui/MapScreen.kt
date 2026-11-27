package com.fakarni.app.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fakarni.app.data.Reminder
import com.fakarni.app.data.ReminderStore
import com.fakarni.app.data.formatMeters
import com.fakarni.app.geo.GeofenceManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.*

private const val MIN_RADIUS = 50.0
private const val MAX_RADIUS = 2000.0

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(store: ReminderStore, geofences: GeofenceManager) {
    val context = LocalContext.current
    val reminders by store.reminders.collectAsStateWithLifecycle()
    val perms = rememberPermissionState(geofences) { geofences.sync(store.all()) }

    var draftCenter by remember { mutableStateOf<LatLng?>(null) }
    var draftRadius by remember { mutableDoubleStateOf(150.0) }
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var showList by remember { mutableStateOf(false) }

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(36.8065, 10.1815), 13f) // GUESS: Tunis until GPS
    }

    // Jump to the user's position once, when location is allowed.
    LaunchedEffect(perms.foreground) {
        if (perms.foreground) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) camera.position = CameraPosition.fromLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
                }
        }
    }

    // The white handle on the circle's edge. Long-press it, then drag to resize.
    val handle = rememberMarkerState()
    LaunchedEffect(draftCenter, draftRadius) {
        val c = draftCenter ?: return@LaunchedEffect
        if (!handle.isDragging) handle.position = offsetEast(c, draftRadius)
    }
    LaunchedEffect(handle.position, handle.isDragging) {
        val c = draftCenter ?: return@LaunchedEffect
        if (handle.isDragging) draftRadius = distance(c, handle.position).coerceIn(MIN_RADIUS, MAX_RADIUS)
        else handle.position = offsetEast(c, draftRadius)
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = MapProperties(isMyLocationEnabled = perms.foreground),
            uiSettings = MapUiSettings(myLocationButtonEnabled = perms.foreground, zoomControlsEnabled = false),
            onMapClick = { draftCenter = it },
        ) {
            reminders.forEach { r ->
                val color = if (r.enabled) Color(0xFFFF9500) else Color.Gray
                Circle(
                    center = LatLng(r.lat, r.lng),
                    radius = r.radius,
                    fillColor = color.copy(alpha = 0.18f),
                    strokeColor = color,
                    strokeWidth = 4f,
                    clickable = true,
                    onClick = { editing = r },
                )
            }
            draftCenter?.let { c ->
                Circle(center = c, radius = draftRadius,
                    fillColor = Color(0x333478F6), strokeColor = Color(0xFF3478F6), strokeWidth = 5f)
                Marker(state = MarkerState(c),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                Marker(state = handle, draggable = true, anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                    zIndex = 10f)
            }
        }

        // Reminder list button
        FilledTonalButton(
            onClick = { showList = true },
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Reminders")
            Spacer(Modifier.width(6.dp)); Text("${reminders.size}")
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!perms.foreground) {
                Banner("Allow location so the map can show where you are.", "Allow", perms.requestForeground)
            } else if (!perms.background) {
                Banner("Choose \"Allow all the time\" so reminders work when the app is closed.", "Open", perms.requestBackground)
            }
            if (draftCenter != null) {
                DraftPanel(
                    radius = draftRadius,
                    onRadius = { draftRadius = it },
                    onCancel = { draftCenter = null },
                    onNext = {
                        val c = draftCenter!!
                        editing = Reminder(lat = c.latitude, lng = c.longitude, radius = draftRadius)
                    },
                )
            } else {
                Text(
                    "Tap the map to choose a place",
                    Modifier.align(Alignment.CenterHorizontally)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    editing?.let { r ->
        ReminderEditor(
            initial = r,
            isNew = store.get(r.id) == null,
            onDismiss = { editing = null },
            onSave = {
                store.save(it); geofences.sync(store.all())
                editing = null; draftCenter = null
            },
            onDelete = { store.delete(r.id); geofences.sync(store.all()); editing = null },
        )
    }
    if (showList) {
        ReminderList(
            reminders = reminders,
            onDismiss = { showList = false },
            onSelect = { r ->
                showList = false
                camera.position = CameraPosition.fromLatLngZoom(LatLng(r.lat, r.lng), 15f)
                editing = r
            },
            onToggle = { r, on -> store.save(r.copy(enabled = on)); geofences.sync(store.all()) },
        )
    }
}

@Composable
private fun DraftPanel(radius: Double, onRadius: (Double) -> Unit, onCancel: () -> Unit, onNext: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row {
                Text("Circle size", Modifier.weight(1f))
                Text("${formatMeters(radius * 2)} across", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // The slider is the precise way; dragging the handle on the map is the quick way.
            Slider(value = radius.toFloat(), onValueChange = { onRadius(it.toDouble()) },
                valueRange = MIN_RADIUS.toFloat()..MAX_RADIUS.toFloat())
            if (radius < 100) Text("Very small circles may trigger late or not at all.",
                color = Color(0xFFE67E00), style = MaterialTheme.typography.bodySmall)
            Text("Tip: long-press the light-blue pin on the edge and drag it.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onNext) { Text("Next") }
            }
        }
    }
}

@Composable
private fun Banner(text: String, action: String, onClick: () -> Unit) {
    Surface(color = Color(0xFFFFE08A), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.Black)
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}

// --- small geo helpers (meters, spherical Earth) ---
private const val EARTH_R = 6_371_000.0

private fun distance(a: LatLng, b: LatLng): Double {
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLng / 2).pow(2)
    return 2 * EARTH_R * asin(sqrt(h))
}

private fun offsetEast(c: LatLng, meters: Double): LatLng {
    val dLng = Math.toDegrees(meters / (EARTH_R * cos(Math.toRadians(c.latitude))))
    return LatLng(c.latitude, c.longitude + dLng)
}
