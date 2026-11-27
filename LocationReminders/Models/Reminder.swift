import Foundation
import CoreLocation

/// One reminder: a circle on the map, a message, and optional time rules.
struct Reminder: Identifiable, Codable, Equatable {
    var id = UUID()
    var title: String
    var note: String = ""
    var latitude: Double
    var longitude: Double
    /// Radius in meters. The UI shows the diameter (2 × radius), since that is how people
    /// usually describe "the size of the circle".
    var radius: Double
    var isEnabled = true

    /// Time rules (all optional). The reminder fires only when you enter the circle
    /// and these rules allow it.
    var timeWindow: TimeWindow?
    /// If set, the reminder is only active on this calendar day.
    var onlyOnDate: Date?
    /// Fire once, then switch the reminder off. Otherwise it fires every time you enter.
    var fireOnce = true

    var lastFiredAt: Date?

    var coordinate: CLLocationCoordinate2D {
        get { .init(latitude: latitude, longitude: longitude) }
        set { latitude = newValue.latitude; longitude = newValue.longitude }
    }

    var diameter: Double { radius * 2 }

    /// Is the reminder allowed to fire right now (ignoring location)?
    func isActive(at date: Date = .now, calendar: Calendar = .current) -> Bool {
        guard isEnabled else { return false }
        if let day = onlyOnDate, !calendar.isDate(day, inSameDayAs: date) { return false }
        if let window = timeWindow, !window.contains(date, calendar: calendar) { return false }
        // Don't fire twice within 5 minutes (GPS can bounce in and out at the edge).
        if let last = lastFiredAt, date.timeIntervalSince(last) < 5 * 60 { return false }
        return true
    }
}

/// A daily time range, e.g. 08:00–10:00. Ranges that cross midnight (22:00–02:00) work too.
struct TimeWindow: Codable, Equatable {
    var startMinutes: Int   // minutes after midnight
    var endMinutes: Int

    func contains(_ date: Date, calendar: Calendar = .current) -> Bool {
        let c = calendar.dateComponents([.hour, .minute], from: date)
        let m = (c.hour ?? 0) * 60 + (c.minute ?? 0)
        if startMinutes <= endMinutes { return m >= startMinutes && m <= endMinutes }
        return m >= startMinutes || m <= endMinutes
    }

    static func minutes(from date: Date, calendar: Calendar = .current) -> Int {
        let c = calendar.dateComponents([.hour, .minute], from: date)
        return (c.hour ?? 0) * 60 + (c.minute ?? 0)
    }

    static func date(fromMinutes m: Int, calendar: Calendar = .current) -> Date {
        calendar.date(bySettingHour: m / 60, minute: m % 60, second: 0, of: .now) ?? .now
    }
}
