package com.fakarni.app.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** One reminder: a circle on the map, a message, and optional time rules. */
data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val note: String = "",
    val lat: Double,
    val lng: Double,
    /** Radius in meters. The app shows the diameter (2 × radius), "300 m across". */
    val radius: Double,
    val enabled: Boolean = true,
    /** Only between these times each day (minutes after midnight). Both null = any time. */
    val fromMinutes: Int? = null,
    val toMinutes: Int? = null,
    /** Only on this day (epoch day). Null = any day. */
    val onlyOnEpochDay: Long? = null,
    /** Fire once, then switch off. Otherwise every time you enter. */
    val fireOnce: Boolean = true,
    val lastFiredAtMillis: Long? = null,
) {
    val diameter: Double get() = radius * 2

    /** Is the reminder allowed to fire right now (ignoring location)? */
    fun isActiveNow(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!enabled) return false
        val local = now.atZone(zone)
        if (onlyOnEpochDay != null && local.toLocalDate().toEpochDay() != onlyOnEpochDay) return false
        if (fromMinutes != null && toMinutes != null) {
            val m = local.hour * 60 + local.minute
            val inside = if (fromMinutes <= toMinutes) m in fromMinutes..toMinutes
                         else m >= fromMinutes || m <= toMinutes   // e.g. 22:00–02:00
            if (!inside) return false
        }
        // GPS can bounce in and out at the edge; ignore repeats within 5 minutes.
        lastFiredAtMillis?.let { if (now.toEpochMilli() - it < 5 * 60_000) return false }
        return true
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("note", note)
        put("lat", lat); put("lng", lng); put("radius", radius)
        put("enabled", enabled); put("fireOnce", fireOnce)
        fromMinutes?.let { put("from", it) }
        toMinutes?.let { put("to", it) }
        onlyOnEpochDay?.let { put("day", it) }
        lastFiredAtMillis?.let { put("lastFired", it) }
    }

    companion object {
        fun fromJson(o: JSONObject) = Reminder(
            id = o.getString("id"),
            title = o.optString("title"),
            note = o.optString("note"),
            lat = o.getDouble("lat"),
            lng = o.getDouble("lng"),
            radius = o.getDouble("radius"),
            enabled = o.optBoolean("enabled", true),
            fireOnce = o.optBoolean("fireOnce", true),
            fromMinutes = if (o.has("from")) o.getInt("from") else null,
            toMinutes = if (o.has("to")) o.getInt("to") else null,
            onlyOnEpochDay = if (o.has("day")) o.getLong("day") else null,
            lastFiredAtMillis = if (o.has("lastFired")) o.getLong("lastFired") else null,
        )
    }
}

fun minutesToTime(m: Int): LocalTime = LocalTime.of(m / 60, m % 60)
fun epochDayToDate(d: Long): LocalDate = LocalDate.ofEpochDay(d)
fun formatMeters(m: Double): String =
    if (m >= 1000) String.format("%.1f km", m / 1000) else "${m.toInt()} m"
