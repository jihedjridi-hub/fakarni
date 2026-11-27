package com.fakarni.app.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fakarni.app.FakarniApp
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** Called by Play services when you enter a reminder circle. */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val app = context.applicationContext as FakarniApp
        var changed = false
        event.triggeringGeofences.orEmpty().forEach { fence ->
            val r = app.store.get(fence.requestId) ?: return@forEach
            if (!r.isActiveNow()) return@forEach
            Notifications.show(context, r)
            app.store.markFired(r.id)
            changed = changed || r.fireOnce
        }
        if (changed) app.geofences.sync(app.store.all())
    }
}

/** Android forgets geofences after a restart or app update; add them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as FakarniApp
        app.geofences.sync(app.store.all())
    }
}
