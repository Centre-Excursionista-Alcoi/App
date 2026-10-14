import ComposeApp
import Foundation

extension KotlinInstant {
    /// The same moment, as Foundation sees it.
    var date: Date { Date(timeIntervalSince1970: Double(toEpochMilliseconds()) / 1000) }
}

extension Kotlinx_datetimeLocalDate {
    /// The day, at the start of it in the user's calendar.
    var date: Date {
        Calendar.current.date(from: DateComponents(year: Int(year), month: Int(monthNumber), day: Int(day))) ?? Date()
    }
}
