import SwiftUI

struct ReminderEditor: View {
    @EnvironmentObject private var store: ReminderStore
    @Environment(\.dismiss) private var dismiss

    @State var reminder: Reminder
    var onSaved: () -> Void = {}

    @State private var useTimeWindow = false
    @State private var windowStart = TimeWindow.date(fromMinutes: 8 * 60)
    @State private var windowEnd = TimeWindow.date(fromMinutes: 20 * 60)
    @State private var useDate = false
    @State private var date = Date.now

    private var isNew: Bool { store.reminder(id: reminder.id) == nil }

    var body: some View {
        NavigationStack {
            Form {
                Section("Reminder") {
                    TextField("What should I remind you?", text: $reminder.title)
                    TextField("Note (optional)", text: $reminder.note, axis: .vertical)
                }
                Section {
                    LabeledContent("Circle size", value: "\(formatMeters(reminder.diameter)) across")
                    Slider(value: $reminder.radius, in: 50...2_000, step: 10)
                } header: { Text("Place") }

                Section {
                    Toggle("Only between certain hours", isOn: $useTimeWindow)
                    if useTimeWindow {
                        DatePicker("From", selection: $windowStart, displayedComponents: .hourAndMinute)
                        DatePicker("To", selection: $windowEnd, displayedComponents: .hourAndMinute)
                    }
                    Toggle("Only on a specific day", isOn: $useDate)
                    if useDate {
                        DatePicker("Day", selection: $date, displayedComponents: .date)
                    }
                    Toggle("Remind me only once", isOn: $reminder.fireOnce)
                } header: { Text("When") } footer: {
                    Text("You'll be reminded when you enter the circle and these rules match.")
                }

                if !isNew {
                    Section {
                        Toggle("On", isOn: $reminder.isEnabled)
                        Button("Delete reminder", role: .destructive) {
                            store.delete(reminder); dismiss()
                        }
                    }
                }
            }
            .navigationTitle(isNew ? "New reminder" : "Edit reminder")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save", action: save)
                        .disabled(reminder.title.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .onAppear(perform: loadRules)
        }
        .presentationDetents([.medium, .large])
    }

    private func loadRules() {
        if let w = reminder.timeWindow {
            useTimeWindow = true
            windowStart = TimeWindow.date(fromMinutes: w.startMinutes)
            windowEnd = TimeWindow.date(fromMinutes: w.endMinutes)
        }
        if let d = reminder.onlyOnDate { useDate = true; date = d }
    }

    private func save() {
        reminder.timeWindow = useTimeWindow
            ? TimeWindow(startMinutes: TimeWindow.minutes(from: windowStart),
                         endMinutes: TimeWindow.minutes(from: windowEnd))
            : nil
        reminder.onlyOnDate = useDate ? date : nil
        if reminder.isEnabled == false && isNew { reminder.isEnabled = true }
        store.save(reminder)
        onSaved()
        dismiss()
    }
}
