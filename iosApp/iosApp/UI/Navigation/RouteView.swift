import ComposeApp
import SwiftUI

/// The screen for a pushed [Route]: the native one where there is one, otherwise the Compose screen.
struct RouteView: View {
    @Environment(ShellModel.self) private var shell
    let route: Route

    var body: some View {
        switch onEnum(of: route.destination) {
        case .settings:
            SettingsView(onDeleteAccount: shell.startLoading, onSecurity: { shell.push(DestinationSecurity.shared) })
        case .security:
            SecurityView()
        default:
            ComposeDestinationIsland(
                destination: route.destination,
                onSessionChange: shell.sessionChanged,
                onClose: shell.closeTop
            )
            .ignoresSafeArea(.container, edges: .bottom)
            .toolbar(.hidden, for: .navigationBar)
        }
    }
}
