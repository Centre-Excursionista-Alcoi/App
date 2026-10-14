import ComposeApp
import SwiftUI

/// The existing Compose UI for one tab, for the tabs that don't have a native version yet.
struct ComposeTabIsland: UIViewControllerRepresentable {
    typealias UIViewControllerType = UIViewController
    typealias Coordinator = Void

    let tab: MainTab
    var scrollToFemecvAccount = false
    let onNavigate: (any Destination) -> Void
    let onTabRequested: (MainTab) -> Void

    func makeUIViewController(context: UIViewControllerRepresentableContext<ComposeTabIsland>) -> UIViewController {
        ComposeIslandsKt.composeTabViewController(
            tab: tab,
            scrollToFemecvAccount: scrollToFemecvAccount,
            onNavigate: onNavigate,
            onTabRequested: onTabRequested
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: UIViewControllerRepresentableContext<ComposeTabIsland>) {}
}

/// The existing Compose UI for one screen (and whatever it opens from there), for the screens that don't have a
/// native version yet.
struct ComposeDestinationIsland: UIViewControllerRepresentable {
    typealias UIViewControllerType = UIViewController
    typealias Coordinator = Void

    let destination: any Destination
    let onSessionChange: (any Destination) -> Void
    let onClose: () -> Void

    func makeUIViewController(context: UIViewControllerRepresentableContext<ComposeDestinationIsland>) -> UIViewController {
        ComposeIslandsKt.composeDestinationViewController(
            destination: destination,
            onSessionChange: onSessionChange,
            onClose: onClose
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: UIViewControllerRepresentableContext<ComposeDestinationIsland>) {}
}

/// The spaces of the lending page, still in Compose.
struct ComposeSpacesIsland: UIViewControllerRepresentable {
    typealias UIViewControllerType = UIViewController
    typealias Coordinator = Void

    let hasItemsSelected: Bool
    let onNavigate: (any Destination) -> Void

    func makeUIViewController(context: UIViewControllerRepresentableContext<ComposeSpacesIsland>) -> UIViewController {
        ComposeIslandsKt.composeSpacesViewController(hasItemsSelected: hasItemsSelected, onNavigate: onNavigate)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: UIViewControllerRepresentableContext<ComposeSpacesIsland>) {}
}
