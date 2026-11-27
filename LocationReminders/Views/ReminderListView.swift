import SwiftUI

struct ReminderListView: View {
    @EnvironmentObject private var store: ReminderStore
    @Environment(\.dismiss) private var dismiss
    var onSelect: (Reminder) -> Void

    var body: some View {
        NavigationStack {
            List {
                if store.reminders.isEmpty {
                    ContentUnavailableView("No reminders yet", systemImage: "mappin.and.ellipse",
                                           description: Text("Tap a place on the map to add one."))
                }
                ForEach(store.reminders) { r in
                    Button { onSelect(r) } label: {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(r.title).foregroundStyle(.primary)
                                Text(subtitle(r)).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Toggle("", isOn: Binding(
                                get: { r.isEnabled },
                                set: { var c = r; c.isEnabled = $0; store.save(c) }
                            )).labelsHidden()
                        }
                    }
                }
                .onDelete { idx in idx.map { store.reminders[$0] }.forEach(store.delete) }

                if store.reminders.filter(\.isEnabled).count > RegionMonitor.maxRegions {
                    Text("iPhone can watch \(RegionMonitor.maxRegions) places at once. The closest ones are watched.")
                        .font(.caption).foregroundStyle(.orange)
                }
            }
            .navigationTitle("Reminders")
            .toolbar { Button("Done") { dismiss() } }
        }
    }

    private func subtitle(_ r: Reminder) -> String {
        var parts = ["\(formatMeters(r.diameter)) across"]
        if let w = r.timeWindow {
            let f = DateFormatter(); f.timeStyle = .short
            parts.append("\(f.string(from: TimeWindow.date(fromMinutes: w.startMinutes)))–\(f.string(from: TimeWindow.date(fromMinutes: w.endMinutes)))")
        }
        if let d = r.onlyOnDate { parts.append(d.formatted(date: .abbreviated, time: .omitted)) }
        return parts.joined(separator: " · ")
    }
}
