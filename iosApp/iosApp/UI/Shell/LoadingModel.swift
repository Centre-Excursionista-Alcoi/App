import ComposeApp
import Observation

@MainActor
@Observable
final class LoadingModel {
    @ObservationIgnored private let viewModel = ViewModelFactory.shared.loadingViewModel()
    @ObservationIgnored private let onLoggedIn: () -> Void
    @ObservationIgnored private let onNotLoggedIn: () -> Void

    private(set) var error: String?
    /// 0...1, or `nil` when it can't be told.
    private(set) var progress: Double?

    @ObservationIgnored private var tasks: [Task<Void, Never>] = []

    init(onLoggedIn: @escaping () -> Void, onNotLoggedIn: @escaping () -> Void) {
        self.onLoggedIn = onLoggedIn
        self.onNotLoggedIn = onNotLoggedIn

        tasks.append(Task { [weak self] in
            guard let flow = self?.viewModel.error else { return }
            for await error in flow {
                self?.error = error.map { $0.message ?? String(describing: type(of: $0)) }
            }
        })
        tasks.append(Task { [weak self] in
            guard let flow = self?.viewModel.progress else { return }
            for await progress in flow {
                if let transfer = progress as? ComposeApp.Progress.Transfer, !transfer.isIndeterminate {
                    self?.progress = transfer.percentage.map { Double(truncating: $0) }
                } else {
                    self?.progress = nil
                }
            }
        })
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }

    func load() {
        _ = viewModel.load(
            onLoggedIn: { [onLoggedIn] in Task { @MainActor in onLoggedIn() } },
            onNotLoggedIn: { [onNotLoggedIn] in Task { @MainActor in onNotLoggedIn() } }
        )
    }
}

@MainActor
final class LogoutModel {
    private let viewModel: LogoutViewModel

    init(afterLogout: @escaping () -> Void) {
        // Logging out starts as soon as the ViewModel is created
        viewModel = ViewModelFactory.shared.logoutViewModel(afterLogout: { Task { @MainActor in afterLogout() } })
    }

    deinit {
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }
}
