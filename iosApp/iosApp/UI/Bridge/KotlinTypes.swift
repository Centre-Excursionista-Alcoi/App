import ComposeApp
import Foundation

extension KotlinInstant {
    /// The same moment, as Foundation sees it.
    var date: Date { Date(timeIntervalSince1970: Double(toEpochMilliseconds()) / 1000) }
}
