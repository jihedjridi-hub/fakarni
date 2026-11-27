import CoreLocation

/// Asks for location permission and tracks the current position for the map.
@MainActor
final class LocationService: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = LocationService()

    @Published var authorization: CLAuthorizationStatus
    @Published var lastLocation: CLLocation?

    private let manager = CLLocationManager()

    override private init() {
        authorization = manager.authorizationStatus
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    /// iOS asks in two steps: first "While Using", then later offers "Always".
    /// Reminders need "Always" to work while the app is closed.
    func requestPermission() {
        switch manager.authorizationStatus {
        case .notDetermined: manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse: manager.requestAlwaysAuthorization()
        default: break
        }
    }

    func startUpdating() { manager.startUpdatingLocation() }

    var hasAlways: Bool { authorization == .authorizedAlways }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            self.authorization = status
            if status == .authorizedWhenInUse || status == .authorizedAlways {
                self.startUpdating()
            }
            if status == .authorizedWhenInUse {
                // Step 2: ask for Always so reminders work in the background.
                self.manager.requestAlwaysAuthorization()
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let loc = locations.last
        Task { @MainActor in self.lastLocation = loc }
    }
}
