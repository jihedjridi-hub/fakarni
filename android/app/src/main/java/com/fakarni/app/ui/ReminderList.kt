package com.fakarni.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fakarni.app.data.*
import com.fakarni.app.geo.GeofenceManager
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderList(
    reminders: List<Reminder>,
    onDismiss: () -> Unit,
    onSelect: (Reminder) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Reminders", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        if (reminders.isEmpty()) {
            Text("No reminders yet. Tap a place on the map to add one.",
                Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(reminders, key = { it.id }) { r ->
                ListItem(
                    headlineContent = { Text(r.title) },
                    supportingContent = { Text(subtitle(r)) },
                    trailingContent = { Switch(r.enabled, { onToggle(r, it) }) },
                    modifier = Modifier.clickable { onSelect(r) },
                )
            }
            if (reminders.count { it.enabled } > GeofenceManager.MAX) item {
                Text("Android can watch ${GeofenceManager.MAX} places at once; extra ones are ignored.",
                    Modifier.padding(20.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun subtitle(r: Reminder): String {
    val parts = mutableListOf("${formatMeters(r.diameter)} across")
    val t = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    if (r.fromMinutes != null && r.toMinutes != null)
        parts += "${minutesToTime(r.fromMinutes).format(t)}–${minutesToTime(r.toMinutes).format(t)}"
    r.onlyOnEpochDay?.let { parts += epochDayToDate(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
    return parts.joinToString(" · ")
}
