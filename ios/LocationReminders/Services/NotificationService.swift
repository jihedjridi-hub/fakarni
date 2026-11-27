import UserNotifications

/// Shows the reminder as a normal iPhone notification.
final class NotificationService: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationService()

    func configure() {
        UNUserNotificationCenter.current().delegate = self
    }

    func requestPermission() async -> Bool {
        (try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    func deliver(_ reminder: Reminder) {
        let content = UNMutableNotificationContent()
        content.title = reminder.title
        content.body = reminder.note.isEmpty ? "You've arrived." : reminder.note
        content.sound = .default
        let request = UNNotificationRequest(identifier: reminder.id.uuidString + "-\(Date.now.timeIntervalSince1970)",
                                            content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }

    // Show the banner even if the app is open.
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
