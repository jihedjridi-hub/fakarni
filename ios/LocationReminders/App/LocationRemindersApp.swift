import SwiftUI
import GoogleMaps

@main
struct LocationRemindersApp: App {
    @StateObject private var store = ReminderStore.shared
    @StateObject private var location = LocationService.shared

    init() {
        let key = Bundle.main.object(forInfoDictionaryKey: "GMSApiKey") as? String ?? ""
        if key.isEmpty || key == "PASTE_YOUR_KEY_HERE" {
            print("⚠️ Google Maps API key missing. See README.md, step 2.")
        }
        GMSServices.provideAPIKey(key)
        // Start listening for "entered area" events as early as possible, so iOS can
        // relaunch the app in the background and we still deliver the reminder.
        RegionMonitor.shared.start()
        NotificationService.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(location)
        }
    }
}
