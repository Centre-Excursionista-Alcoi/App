import ComposeApp
import SwiftUI

/// The app's root: what is shown depends on where [ShellModel] says the user is.
struct RootView: View {
    @State private var shell = ShellModel()

    var body: some View {
        Group {
            switch shell.stage {
            case .initializing:
                ProgressView()
            case .loading:
                LoadingView(onLoggedIn: shell.loggedIn, onNotLoggedIn: shell.notLoggedIn)
            case .login(let changedPassword):
                AuthView(changedPassword: changedPassword, afterLogin: shell.startLoading)
                    .id(changedPassword)
            case .main:
                MainView()
            case .logout:
                LogoutView(afterLogout: shell.startLoading)
            }
        }
        .environment(shell)
        .tint(Brand.primary)
    }
}
