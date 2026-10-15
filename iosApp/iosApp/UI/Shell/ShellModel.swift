import ComposeApp
import Observation
import SwiftUI

/// Where the app is: starting up, loading the user's data, signing in, or in. Mirrors `App.kt`, which does the same
/// for the Compose app, and leaves every decision (is the user logged in? where does a link go?) to the shared code.
@MainActor
@Observable
final class ShellModel {
    enum Stage: Equatable {
        case initializing
        case loading
        case login(changedPassword: Bool)
        case main
        case logout
    }

    private(set) var stage: Stage = .initializing

    /// The screens open over the tabs.
    var path: [Route] = []
    var selectedTab: MainTab = .home
    /// The profile is shown scrolled to the FEMECV account (its credentials are wrong).
    var scrollToFemecvAccount = false

    @ObservationIgnored private let initializer = ViewModelFactory.shared.platformInitializerViewModel()
    @ObservationIgnored private var sessionExpiry: SessionExpiryViewModel?
    @ObservationIgnored private var tasks: [Task<Void, Never>] = []
    @ObservationIgnored private var openAfterLoad: (any Destination)?
    @ObservationIgnored private var waitingLink: (any Destination)?

    init() {
        tasks.append(Task { [weak self] in
            guard let initializer = self?.initializer else { return }
            for await ready in initializer.isReady where ready.boolValue {
                self?.finishInitializing()
                break
            }
        })
        tasks.append(Task { [weak self] in
            for await url in DeepLinks.shared.pending {
                guard let url else { continue }
                await self?.open(link: url)
            }
        })
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: initializer)
    }

    // MARK: Transitions

    private func finishInitializing() {
        openAfterLoad = initializer.targetDestination.value
        stage = .loading
    }

    func loggedIn() {
        // A link the user opened wins over what a notification would open
        let opening = waitingLink ?? openAfterLoad
        waitingLink = nil
        openAfterLoad = nil

        path = []
        selectedTab = DebugLaunch.tab ?? .home
        stage = .main
        startWatchingSession()
        if let opening { show(opening) }
        if let debugDestination = DebugLaunch.destination { push(debugDestination) }
    }

    func notLoggedIn() {
        stage = .login(changedPassword: false)
    }

    func startLoading() {
        sessionExpiry.map(ViewModelFactory.shared.dispose(viewModel:))
        sessionExpiry = nil
        stage = .loading
    }

    func requestLogout() {
        stage = .logout
    }

    /// A Compose island sends the user somewhere it can't show: back to loading after logging in or out, or to the
    /// login screen.
    func sessionChanged(to destination: any Destination) {
        switch onEnum(of: destination) {
        case .login(let login): stage = .login(changedPassword: login.changedPassword)
        default: startLoading()
        }
    }

    private func startWatchingSession() {
        sessionExpiry = ViewModelFactory.shared.sessionExpiryViewModel(onLoggedOut: { [weak self] in
            Task { @MainActor in self?.stage = .login(changedPassword: false) }
        })
    }

    // MARK: Links and navigation

    private func open(link url: Ktor_httpUrl) async {
        let destination = try? await DeepLinks.shared.fromUrl(url: url)
        DeepLinks.shared.consume(url: url)
        guard let destination else { return }

        switch stage {
        case .main:
            show(destination)
        case .initializing, .loading:
            // Nowhere to open it yet: it opens once the user is in
            waitingLink = destination
        case .login(let changedPassword):
            if case .login = onEnum(of: destination) { stage = .login(changedPassword: changedPassword) } else { waitingLink = destination }
        case .logout:
            waitingLink = destination
        }
    }

    /// Opens [destination] with the screens before it, as if the user had got there normally (see
    /// `Destination.backStackFor`).
    private func show(_ destination: any Destination) {
        for step in DestinationCompanion.shared.backStackFor(destination: destination) {
            switch onEnum(of: step) {
            case .main(let main):
                if main.showingAdminLendingsScreen || main.showingAdminItemTypeId != nil {
                    selectedTab = .management
                } else if main.showingFemecvAccount {
                    selectedTab = .profile
                    scrollToFemecvAccount = true
                }
            default:
                push(step)
            }
        }
    }

    func push(_ destination: any Destination) {
        path.append(Route(destination: destination))
    }

    func closeTop() {
        if !path.isEmpty { path.removeLast() }
    }
}
