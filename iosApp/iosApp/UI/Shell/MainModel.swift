import ComposeApp
import Observation

/// What the tabs need from the shared `MainScreenViewModel`: which tabs there are, the badge, syncing.
@MainActor
@Observable
final class MainModel {
    @ObservationIgnored private let viewModel = ViewModelFactory.shared.mainScreenViewModel()
    @ObservationIgnored private var tasks: [Task<Void, Never>] = []

    private(set) var tabs: [MainTab] = [.home]
    private(set) var managementBadge = 0
    private(set) var isSyncing = false
    private(set) var isAdmin = false

    init() {
        tasks.append(Task { [weak self] in
            guard let flow = self?.viewModel.tabs else { return }
            for await tabs in flow { if let tabs { self?.tabs = tabs } }
        })
        tasks.append(Task { [weak self] in
            guard let flow = self?.viewModel.managementBadge else { return }
            for await count in flow { self?.managementBadge = count.intValue }
        })
        tasks.append(Task { [weak self] in
            guard let flow = self?.viewModel.isSyncing else { return }
            for await syncing in flow { self?.isSyncing = syncing?.boolValue ?? false }
        })
        tasks.append(Task { [weak self] in
            guard let flow = self?.viewModel.profile else { return }
            for await profile in flow { self?.isAdmin = profile?.isAdmin ?? false }
        })
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }

    func sync() {
        _ = viewModel.sync()
    }
}
