import ComposeApp
import Observation
import UIKit

/// Wraps the shared `LoginViewModel`: signing in, registering and every step in between are Kotlin's.
@MainActor
@Observable
final class AuthModel {
    enum RegistrationStep: Equatable {
        case email
        case code(email: String)
        case method(email: String, code: String)
    }

    private(set) var isLoading = false
    private(set) var error: AuthError?
    private(set) var registrationStep: RegistrationStep = .email
    /// The code the user compares with the one their browser shows, while they sign in there.
    private(set) var browserCode: String?
    /// An account was already saved on this device when this screen was reached.
    private(set) var existingAccountEmail: String?

    let canUseSavedCredentials: Bool
    let canSignInWithBrowser: Bool
    let canRegisterWithPasskey: Bool

    @ObservationIgnored private let viewModel = ViewModelFactory.shared.loginViewModel()
    @ObservationIgnored private let afterLogin: () -> Void
    @ObservationIgnored private var tasks: [Task<Void, Never>] = []

    init(afterLogin: @escaping () -> Void) {
        self.afterLogin = afterLogin
        canUseSavedCredentials = viewModel.canUseSavedCredentials
        canSignInWithBrowser = viewModel.canSignInWithBrowser
        canRegisterWithPasskey = viewModel.canRegisterWithPasskey
        existingAccountEmail = viewModel.existingAccountEmail.value

        tasks = [
            collect(viewModel.isLoading) { [weak self] in self?.isLoading = $0.boolValue },
            collect(viewModel.authError) { [weak self] in self?.error = $0 },
            collect(viewModel.browserLogin) { [weak self] in self?.browserCode = $0?.code },
            collect(viewModel.registrationStep) { [weak self] step in
                switch onEnum(of: step) {
                case .email: self?.registrationStep = .email
                case .code(let step): self?.registrationStep = .code(email: step.email)
                case .method(let step): self?.registrationStep = .method(email: step.email, code: step.code)
                }
            },
        ]
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }

    private var done: () -> Void { { [afterLogin] in Task { @MainActor in afterLogin() } } }

    func login(email: String, password: String) {
        _ = viewModel.login(email: email, password: password, isSaved: false, afterLogin: done)
    }

    func signInWithSavedCredential() {
        _ = viewModel.signInWithSavedCredential(afterLogin: done)
    }

    func signInWithBrowser() {
        viewModel.signInWithBrowser(
            openUrl: { url in
                Task { @MainActor in
                    if let url = URL(string: url) { UIApplication.shared.open(url) }
                }
            },
            afterLogin: done
        )
    }

    func cancelBrowserLogin() { viewModel.cancelBrowserLogin() }

    func forgetExistingAccount() {
        _ = viewModel.forgetExistingAccount()
        existingAccountEmail = nil
    }

    func forgotPassword(email: String, afterRequest: @escaping () -> Void) {
        _ = viewModel.forgotPassword(email: email, afterRequest: { Task { @MainActor in afterRequest() } })
    }

    func clearError() { viewModel.clearError() }

    // MARK: Registering

    func requestRegistrationCode(email: String) { _ = viewModel.requestRegistrationCode(email: email) }
    func enterRegistrationCode(_ code: String) { viewModel.enterRegistrationCode(code: code) }
    func registrationBack() { viewModel.registrationBack() }
    func registerWithPasskey() { _ = viewModel.registerWithPasskey(afterLogin: done) }
    func register(password: String) { _ = viewModel.register(password: password, afterLogin: done) }
}
