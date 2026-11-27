package com.fakarni.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fakarni.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditor(
    initial: Reminder,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var radius by remember { mutableDoubleStateOf(initial.radius) }
    var useHours by remember { mutableStateOf(initial.fromMinutes != null) }
    var from by remember { mutableIntStateOf(initial.fromMinutes ?: 8 * 60) }
    var to by remember { mutableIntStateOf(initial.toMinutes ?: 20 * 60) }
    var useDay by remember { mutableStateOf(initial.onlyOnEpochDay != null) }
    var day by remember { mutableLongStateOf(initial.onlyOnEpochDay ?: LocalDate.now().toEpochDay()) }
    var fireOnce by remember { mutableStateOf(initial.fireOnce) }
    var enabled by remember { mutableStateOf(initial.enabled) }

    val timeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun pickTime(current: Int, set: (Int) -> Unit) =
        TimePickerDialog(context, { _, h, m -> set(h * 60 + m) }, current / 60, current % 60,
            android.text.format.DateFormat.is24HourFormat(context)).show()

    fun pickDay() {
        val d = epochDayToDate(day)
        DatePickerDialog(context, { _, y, mo, dd -> day = LocalDate.of(y, mo + 1, dd).toEpochDay() },
            d.year, d.monthValue - 1, d.dayOfMonth).show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (isNew) "New reminder" else "Edit reminder", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("What should I remind you?") })
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Note (optional)") })

            Row { Text("Circle size", Modifier.weight(1f)); Text("${formatMeters(radius * 2)} across") }
            Slider(radius.toFloat(), { radius = it.toDouble() }, valueRange = 50f..2000f)

            HorizontalDivider()
            Text("When", style = MaterialTheme.typography.titleMedium)
            SwitchRow("Only between certain hours", useHours) { useHours = it }
            if (useHours) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ pickTime(from) { from = it } }) { Text("From ${minutesToTime(from).format(timeFmt)}") }
                OutlinedButton({ pickTime(to) { to = it } }) { Text("To ${minutesToTime(to).format(timeFmt)}") }
            }
            SwitchRow("Only on a specific day", useDay) { useDay = it }
            if (useDay) OutlinedButton(::pickDay) { Text(epochDayToDate(day).format(dateFmt)) }
            SwitchRow("Remind me only once", fireOnce) { fireOnce = it }
            Text("You'll be reminded when you enter the circle and these rules match.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!isNew) {
                HorizontalDivider()
                SwitchRow("On", enabled) { enabled = it }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete reminder")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(initial.copy(
                            title = title.trim(), note = note.trim(), radius = radius,
                            fromMinutes = if (useHours) from else null,
                            toMinutes = if (useHours) to else null,
                            onlyOnEpochDay = if (useDay) day else null,
                            fireOnce = fireOnce,
                            enabled = if (isNew) true else enabled,
                        ))
                    },
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}
