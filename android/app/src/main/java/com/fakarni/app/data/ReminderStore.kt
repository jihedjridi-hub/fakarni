package com.fakarni.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/** Saves reminders on the phone (no account, no server). */
class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private val _reminders = MutableStateFlow(load())
    val reminders: StateFlow<List<Reminder>> = _reminders

    fun all(): List<Reminder> = _reminders.value
    fun get(id: String): Reminder? = all().firstOrNull { it.id == id }

    fun save(r: Reminder) {
        val list = all().toMutableList()
        val i = list.indexOfFirst { it.id == r.id }
        if (i >= 0) list[i] = r else list.add(r)
        persist(list)
    }

    fun delete(id: String) = persist(all().filterNot { it.id == id })

    fun markFired(id: String) {
        val r = get(id) ?: return
        save(r.copy(lastFiredAtMillis = System.currentTimeMillis(), enabled = if (r.fireOnce) false else r.enabled))
    }

    private fun load(): List<Reminder> {
        val raw = prefs.getString("list", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { Reminder.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<Reminder>) {
        val arr = JSONArray().apply { list.forEach { put(it.toJson()) } }
        prefs.edit().putString("list", arr.toString()).apply()
        _reminders.value = list
    }
}
