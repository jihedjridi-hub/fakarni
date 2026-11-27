package com.fakarni.app

import android.app.Application
import com.fakarni.app.data.ReminderStore
import com.fakarni.app.geo.GeofenceManager
import com.fakarni.app.geo.Notifications

class FakarniApp : Application() {
    lateinit var store: ReminderStore
        private set
    lateinit var geofences: GeofenceManager
        private set

    override fun onCreate() {
        super.onCreate()
        store = ReminderStore(this)
        geofences = GeofenceManager(this)
        Notifications.createChannel(this)
        geofences.sync(store.all())
    }
}
