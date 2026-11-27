import Foundation

/// Saves reminders as a JSON file on the phone. No account, no server.
@MainActor
final class ReminderStore: ObservableObject {
    static let shared = ReminderStore()

    @Published private(set) var reminders: [Reminder] = []

    private let fileURL = FileManager.default
        .urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        .appendingPathComponent("reminders.json")

    private init() { load() }

    func reminder(id: UUID) -> Reminder? { reminders.first { $0.id == id } }

    func save(_ reminder: Reminder) {
        if let i = reminders.firstIndex(where: { $0.id == reminder.id }) {
            reminders[i] = reminder
        } else {
            reminders.append(reminder)
        }
        persist()
        RegionMonitor.shared.sync(with: reminders)
    }

    func delete(_ reminder: Reminder) {
        reminders.removeAll { $0.id == reminder.id }
        persist()
        RegionMonitor.shared.sync(with: reminders)
    }

    func markFired(id: UUID) {
        guard var r = reminder(id: id) else { return }
        r.lastFiredAt = .now
        if r.fireOnce { r.isEnabled = false }
        save(r)
    }

    private func load() {
        guard let data = try? Data(contentsOf: fileURL) else { return }
        reminders = (try? JSONDecoder().decode([Reminder].self, from: data)) ?? []
    }

    private func persist() {
        do {
            try FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(),
                                                    withIntermediateDirectories: true)
            try JSONEncoder().encode(reminders).write(to: fileURL, options: .atomic)
        } catch {
            print("Could not save reminders: \(error)")
        }
    }
}
