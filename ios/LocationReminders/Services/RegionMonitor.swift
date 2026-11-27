import CoreLocation

/// Watches the reminder circles with Apple's CLMonitor (iOS 17+). iOS does the watching,
/// even when the app is closed, and wakes the app up when you walk into a circle.
///
/// Limit: iOS watches at most 20 circles per app at once. Circles smaller than about
/// 100 m are less reliable, so the app suggests 100 m as the minimum radius.
final class RegionMonitor: @unchecked Sendable {
    static let shared = RegionMonitor()
    static let maxRegions = 20

    private var monitor: CLMonitor?
    private var listenTask: Task<Void, Never>?

    func start() {
        guard listenTask == nil else { return }
        listenTask = Task {
            let monitor = await CLMonitor("LocationReminders")
            self.monitor = monitor
            let reminders = await ReminderStore.shared.reminders
            await self.apply(reminders, to: monitor)
            do {
                for try await event in await monitor.events {
                    guard event.state == .satisfied, let id = UUID(uuidString: event.identifier) else { continue }
                    await self.handleEntered(reminderID: id)
                }
            } catch {
                print("Region monitoring stopped: \(error)")
            }
        }
    }

    func sync(with reminders: [Reminder]) {
        Task {
            guard let monitor else { return }
            await apply(reminders, to: monitor)
        }
    }

    private func apply(_ reminders: [Reminder], to monitor: CLMonitor) async {
        // Only watch reminders that are switched on. If there are more than 20,
        // watch the 20 closest to where the user is (simple first-version rule).
        var wanted = reminders.filter(\.isEnabled)
        if wanted.count > Self.maxRegions, let here = await LocationService.shared.lastLocation {
            wanted.sort {
                CLLocation(latitude: $0.latitude, longitude: $0.longitude).distance(from: here) <
                CLLocation(latitude: $1.latitude, longitude: $1.longitude).distance(from: here)
            }
        }
        wanted = Array(wanted.prefix(Self.maxRegions))
        let wantedIDs = Set(wanted.map(\.id.uuidString))

        for id in await monitor.identifiers where !wantedIDs.contains(id) {
            await monitor.remove(id)
        }
        for r in wanted {
            let condition = CLMonitor.CircularGeographicCondition(center: r.coordinate, radius: r.radius)
            // add() replaces an existing condition with the same identifier, so edits apply.
            await monitor.add(condition, identifier: r.id.uuidString, assuming: .unsatisfied)
        }
    }

    @MainActor
    private func handleEntered(reminderID: UUID) {
        guard let reminder = ReminderStore.shared.reminder(id: reminderID),
              reminder.isActive() else { return }
        NotificationService.shared.deliver(reminder)
        ReminderStore.shared.markFired(id: reminderID)
    }
}
