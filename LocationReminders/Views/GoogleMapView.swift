import SwiftUI
import GoogleMaps

/// Google Maps inside SwiftUI.
/// - Tap the map to place the center of a new circle.
/// - Press and hold the white handle on the circle's edge, then drag it to resize.
/// - Tap an existing circle to edit that reminder.
struct GoogleMapView: UIViewRepresentable {
    let reminders: [Reminder]
    @Binding var draftCenter: CLLocationCoordinate2D?
    @Binding var draftRadius: Double
    @Binding var cameraTarget: CLLocationCoordinate2D?
    var onSelectReminder: (Reminder) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> GMSMapView {
        let options = GMSMapViewOptions()
        options.camera = GMSCameraPosition(latitude: 36.8065, longitude: 10.1815, zoom: 13) // GUESS: Tunis until GPS arrives
        let map = GMSMapView(options: options)
        map.isMyLocationEnabled = true
        map.settings.myLocationButton = true
        map.delegate = context.coordinator
        return map
    }

    func updateUIView(_ map: GMSMapView, context: Context) {
        let c = context.coordinator
        c.parent = self
        c.drawSaved(reminders, on: map)
        c.drawDraft(center: draftCenter, radius: draftRadius, on: map)
        if let target = cameraTarget {
            map.animate(toLocation: target)
            DispatchQueue.main.async { cameraTarget = nil }
        }
    }

    final class Coordinator: NSObject, GMSMapViewDelegate {
        var parent: GoogleMapView
        private var savedCircles: [UUID: GMSCircle] = [:]
        private var savedSnapshot: [Reminder] = []
        private let draftCircle = GMSCircle()
        private let centerMarker = GMSMarker()
        private let handleMarker = GMSMarker()
        private var isDraggingHandle = false

        init(_ parent: GoogleMapView) {
            self.parent = parent
            super.init()
            draftCircle.fillColor = UIColor.systemBlue.withAlphaComponent(0.2)
            draftCircle.strokeColor = .systemBlue
            draftCircle.strokeWidth = 2
            centerMarker.icon = GMSMarker.markerImage(with: .systemBlue)
            handleMarker.icon = Self.handleImage()
            handleMarker.groundAnchor = CGPoint(x: 0.5, y: 0.5)
            handleMarker.isDraggable = true
            handleMarker.zIndex = 10
        }

        // MARK: drawing

        func drawSaved(_ reminders: [Reminder], on map: GMSMapView) {
            guard reminders != savedSnapshot else { return }
            savedSnapshot = reminders
            savedCircles.values.forEach { $0.map = nil }
            savedCircles = [:]
            for r in reminders {
                let circle = GMSCircle(position: r.coordinate, radius: r.radius)
                let color: UIColor = r.isEnabled ? .systemOrange : .systemGray
                circle.fillColor = color.withAlphaComponent(0.18)
                circle.strokeColor = color
                circle.strokeWidth = 1.5
                circle.isTappable = true
                circle.userData = r.id
                circle.title = r.title
                circle.map = map
                savedCircles[r.id] = circle
            }
        }

        func drawDraft(center: CLLocationCoordinate2D?, radius: Double, on map: GMSMapView) {
            guard let center else {
                draftCircle.map = nil; centerMarker.map = nil; handleMarker.map = nil
                return
            }
            draftCircle.position = center
            draftCircle.radius = radius
            draftCircle.map = map
            centerMarker.position = center
            centerMarker.map = map
            if !isDraggingHandle {
                handleMarker.position = GMSGeometryOffset(center, radius, 90)
            }
            handleMarker.map = map
        }

        // MARK: GMSMapViewDelegate

        func mapView(_ mapView: GMSMapView, didTapAt coordinate: CLLocationCoordinate2D) {
            parent.draftCenter = coordinate
        }

        func mapView(_ mapView: GMSMapView, didTap overlay: GMSOverlay) {
            guard let id = overlay.userData as? UUID,
                  let r = parent.reminders.first(where: { $0.id == id }) else { return }
            parent.onSelectReminder(r)
        }

        func mapView(_ mapView: GMSMapView, didBeginDragging marker: GMSMarker) {
            if marker == handleMarker { isDraggingHandle = true }
        }

        func mapView(_ mapView: GMSMapView, didDrag marker: GMSMarker) {
            guard marker == handleMarker, let center = parent.draftCenter else { return }
            let r = GMSGeometryDistance(center, marker.position)
            parent.draftRadius = min(max(r, 50), 2_000)
        }

        func mapView(_ mapView: GMSMapView, didEndDragging marker: GMSMarker) {
            guard marker == handleMarker else { return }
            isDraggingHandle = false
            if let center = parent.draftCenter {
                handleMarker.position = GMSGeometryOffset(center, parent.draftRadius, 90)
            }
        }

        private static func handleImage() -> UIImage {
            let size = CGSize(width: 26, height: 26)
            return UIGraphicsImageRenderer(size: size).image { ctx in
                let rect = CGRect(origin: .zero, size: size).insetBy(dx: 3, dy: 3)
                UIColor.white.setFill()
                UIColor.systemBlue.setStroke()
                let path = UIBezierPath(ovalIn: rect)
                path.lineWidth = 3
                path.fill(); path.stroke()
            }
        }
    }
}
