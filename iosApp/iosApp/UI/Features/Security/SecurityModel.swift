import ComposeApp
import Foundation
import Observation

/// Wraps the shared `SecurityViewModel`: the user's passkeys and password, and the changes to them (each one asks
/// the user to confirm it's them first).
@MainActor
@Observable
final class SecurityModel {
    struct Passkey: Identifiable {
        let id: String
        let name: String?
        let createdAt: Date
        let lastUsedAt: Date?
    }

    enum Change: Equatable {
        case removePasskey(id: String)
        case setPassword(String)
        case removePassword
    }

    @ObservationIgnored private let viewModel = ViewModelFactory.shared.securityViewModel()
    @ObservationIgnored private var tasks: [Task<Void, Never>] = []

    private(set) var hasPassword = false
    private(set) var passkeys: [Passkey] = []
    private(set) var isLoaded = false
    private(set) var isLoading = false
    private(set) var error: AuthError?
    /// A change waiting for the user to confirm it's them.
    private(set) var pendingChange: Change?

    let isPasskeysSupported: Bool

    init() {
        isPasskeysSupported = viewModel.isPasskeysSupported
        tasks = [
            collect(viewModel.info) { [weak self] info in
                guard let info else { return }
                self?.isLoaded = true
                self?.hasPassword = info.hasPassword
                self?.passkeys = info.passkeys.map {
                    Passkey(id: $0.id, name: $0.name, createdAt: $0.createdAt.date, lastUsedAt: $0.lastUsedAt?.date)
                }
            },
            collect(viewModel.isLoading) { [weak self] in self?.isLoading = $0.boolValue },
            collect(viewModel.authError) { [weak self] in self?.error = $0 },
            collect(viewModel.pendingChange) { [weak self] change in
                guard let change else { self?.pendingChange = nil; return }
                switch onEnum(of: change) {
                case .removePasskey(let change): self?.pendingChange = .removePasskey(id: change.id)
                case .setPassword(let change): self?.pendingChange = .setPassword(change.newPassword)
                case .removePassword: self?.pendingChange = .removePassword
                }
            },
        ]
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }

    var canConfirmWithPasskey: Bool { isPasskeysSupported && !passkeys.isEmpty }

    func addPasskey() { _ = viewModel.addPasskey() }

    func request(_ change: Change) {
        switch change {
        case .removePasskey(let id): viewModel.request(change: SecurityChangeRemovePasskey(id: id))
        case .setPassword(let password): viewModel.request(change: SecurityChangeSetPassword(newPassword: password))
        case .removePassword: viewModel.request(change: SecurityChangeRemovePassword.shared)
        }
    }

    func cancelChange() { viewModel.cancelChange() }
    func confirmWithPasskey() { _ = viewModel.confirmWithPasskey() }
    func confirmWithPassword(_ password: String) { _ = viewModel.confirmWithPassword(password: password) }
}
