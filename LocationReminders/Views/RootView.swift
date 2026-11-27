import SwiftUI
import CoreLocation

struct RootView: View {
    @EnvironmentObject private var store: ReminderStore
    @EnvironmentObject private var location: LocationService

    @State private var draftCenter: CLLocationCoordinate2D?
    @State private var draftRadius: Double = 150
    @State private var cameraTarget: CLLocationCoordinate2D?
    @State private var editing: Reminder?
    @State private var showList = false
    @State private var didCenterOnUser = false

    var body: some View {
        ZStack(alignment: .bottom) {
            GoogleMapView(reminders: store.reminders,
                          draftCenter: $draftCenter,
                          draftRadius: $draftRadius,
                          cameraTarget: $cameraTarget,
                          onSelectReminder: { editing = $0 })
                .ignoresSafeArea()

            VStack(spacing: 12) {
                if !location.hasAlways {
                    PermissionBanner { location.requestPermission() }
                }
                if draftCenter != nil {
                    DraftPanel(radius: $draftRadius,
                               onCancel: { draftCenter = nil },
                               onNext: startNewReminder)
                } else {
                    Text("Tap the map to choose a place")
                        .font(.subheadline)
                        .padding(.horizontal, 16).padding(.vertical, 10)
                        .background(.regularMaterial, in: Capsule())
                }
            }
            .padding()
        }
        .overlay(alignment: .topTrailing) {
            Button { showList = true } label: {
                Label("\(store.reminders.count)", systemImage: "list.bullet")
                    .padding(10)
                    .background(.regularMaterial, in: Capsule())
            }
            .padding()
        }
        .sheet(item: $editing) { reminder in
            ReminderEditor(reminder: reminder) { draftCenter = nil }
        }
        .sheet(isPresented: $showList) {
            ReminderListView { r in
                showList = false
                cameraTarget = r.coordinate
                editing = r
            }
        }
        .task {
            location.requestPermission()
            _ = await NotificationService.shared.requestPermission()
        }
        .onChange(of: location.lastLocation) { _, loc in
            if let loc, !didCenterOnUser {
                didCenterOnUser = true
                cameraTarget = loc.coordinate
            }
        }
    }

    private func startNewReminder() {
        guard let center = draftCenter else { return }
        editing = Reminder(title: "", latitude: center.latitude,
                           longitude: center.longitude, radius: draftRadius)
    }
}

private struct DraftPanel: View {
    @Binding var radius: Double
    var onCancel: () -> Void
    var onNext: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Circle size")
                Spacer()
                Text("\(formatMeters(radius * 2)) across").monospacedDigit().foregroundStyle(.secondary)
            }
            // Slider is the precise way; dragging the white handle on the map is the quick way.
            Slider(value: $radius, in: 50...2_000, step: 10)
            if radius < 100 {
                Text("Very small circles may trigger late or not at all.")
                    .font(.caption).foregroundStyle(.orange)
            }
            HStack {
                Button("Cancel", role: .cancel, action: onCancel)
                Spacer()
                Button("Next", action: onNext).buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct PermissionBanner: View {
    var action: () -> Void
    var body: some View {
        HStack {
            Image(systemName: "location.slash")
            Text("Allow location \"Always\" so reminders work when the app is closed.")
                .font(.caption)
            Spacer()
            Button("Allow", action: action).font(.caption.bold())
        }
        .padding(10)
        .background(.yellow.opacity(0.9), in: RoundedRectangle(cornerRadius: 12))
    }
}

func formatMeters(_ m: Double) -> String {
    m >= 1000 ? String(format: "%.1f km", m / 1000) : "\(Int(m)) m"
}
