import ComposeApp
import SwiftUI

/// Loads the user's data: the shared `LoadingViewModel` checks the session, syncs and says where to go next.
struct LoadingView: View {
    let onLoggedIn: () -> Void
    let onNotLoggedIn: () -> Void

    @State private var model: LoadingModel?

    var body: some View {
        VStack(spacing: 16) {
            if let error = model?.error {
                ContentUnavailableView {
                    Label("loading_screen_error", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(error)
                } actions: {
                    Button("retry") { model?.load() }
                        .buttonStyle(.glassProminent)
                }
            } else {
                ProgressView().controlSize(.large)
                if let progress = model?.progress {
                    ProgressView(value: progress)
                        .padding(.horizontal, 48)
                }
            }
        }
        .task {
            let model = LoadingModel(onLoggedIn: onLoggedIn, onNotLoggedIn: onNotLoggedIn)
            self.model = model
            model.load()
        }
    }
}

struct LogoutView: View {
    let afterLogout: () -> Void

    @State private var model: LogoutModel?

    var body: some View {
        ProgressView()
            .controlSize(.large)
            .task { model = LogoutModel(afterLogout: afterLogout) }
    }
}
