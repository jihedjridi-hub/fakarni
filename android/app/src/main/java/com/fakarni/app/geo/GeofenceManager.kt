package com.fakarni.app.geo

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.fakarni.app.data.Reminder
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Hands the reminder circles to Google Play services, which watches them even when the
 * app is closed and wakes [GeofenceReceiver] when you walk in.
 * Limit: 100 circles per app. Circles under ~100 m across are less reliable.
 */
class GeofenceManager(private val context: Context) {
    private val client = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)
        // Geofencing needs a MUTABLE intent so Play services can attach the event.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun hasForegroundLocation() = granted(Manifest.permission.ACCESS_FINE_LOCATION)
    fun hasBackgroundLocation() =
        Build.VERSION.SDK_INT < 29 || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun granted(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    /** Replace all watched circles with the switched-on reminders. */
    @SuppressLint("MissingPermission")
    fun sync(reminders: List<Reminder>) {
        if (!hasForegroundLocation()) return
        val active = reminders.filter { it.enabled }.take(MAX)
        client.removeGeofences(pendingIntent).addOnCompleteListener {
            if (active.isEmpty()) return@addOnCompleteListener
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(0) // don't fire just because you're already inside
                .addGeofences(active.map { r ->
                    Geofence.Builder()
                        .setRequestId(r.id)
                        .setCircularRegion(r.lat, r.lng, r.radius.toFloat())
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build()
                })
                .build()
            client.addGeofences(request, pendingIntent)
                .addOnFailureListener { Log.w("Fakarni", "Could not add geofences", it) }
        }
    }

    companion object { const val MAX = 100 }
}
