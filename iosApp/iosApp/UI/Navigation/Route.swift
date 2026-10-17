import ComposeApp
import Foundation

/// A screen pushed over the tabs: the shared `Destination`, made `Hashable` so `NavigationStack` can hold it.
///
/// Destinations are Kotlin data classes, so their equality and hash code are Kotlin's.
struct Route: Hashable {
    let destination: any Destination

    static func == (lhs: Route, rhs: Route) -> Bool {
        (lhs.destination as? NSObject)?.isEqual(rhs.destination as? NSObject) ?? false
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine((destination as? NSObject)?.hash ?? 0)
    }
}
