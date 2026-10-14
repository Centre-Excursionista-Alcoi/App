import ComposeApp
import SwiftUI

/// Native counterpart of `LoginScreen.kt`: signing in, and registering in three steps (email, emailed code, how the
/// account signs in).
struct AuthView: View {
    let changedPassword: Bool
    let afterLogin: () -> Void

    @State private var model: AuthModel?

    var body: some View {
        Group {
            if let model {
                AuthContent(model: model, changedPassword: changedPassword)
            } else {
                ProgressView()
            }
        }
        .task { if model == nil { model = AuthModel(afterLogin: afterLogin) } }
    }
}

private struct AuthContent: View {
    let model: AuthModel
    let changedPassword: Bool

    @State private var isRegistering = false
    @State private var showingChangedPassword: Bool
    @State private var dismissedExistingAccount = false
    @State private var showingForgotPassword = false
    @State private var forgotPasswordSent = false

    @State private var email = ""
    @State private var password = ""
    @State private var forgotPasswordEmail = ""

    @FocusState private var focus: Field?
    private enum Field { case email, password }

    init(model: AuthModel, changedPassword: Bool) {
        self.model = model
        self.changedPassword = changedPassword
        _showingChangedPassword = State(initialValue: changedPassword)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Image("banner", bundle: nil)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: 480)
                    .accessibilityHidden(true)

                if isRegistering {
                    RegisterForm(model: model, email: $email, onShowLogin: showLogin)
                } else {
                    loginForm
                }

                if let error = model.error {
                    ErrorCard(error: error, passkeysSupported: model.canUseSavedCredentials)
                }
            }
            .frame(maxWidth: 480)
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom) {
            Image("people", bundle: nil)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundStyle(.tint)
                .frame(maxHeight: 120)
                .padding(.horizontal, 36)
                .padding(.bottom, 8)
                .accessibilityHidden(true)
                .allowsHitTesting(false)
        }
        .disabled(model.isLoading && model.browserCode == nil)
        .alert("login_password_changed_title", isPresented: $showingChangedPassword) {
            Button("close") {}
        } message: {
            Text("login_password_changed_message")
        }
        .alert(
            "login_existing_account_title",
            isPresented: .init(
                get: { model.existingAccountEmail != nil && !dismissedExistingAccount },
                set: { if !$0 { dismissedExistingAccount = true } }
            )
        ) {
            Button("login_existing_account_logout", role: .destructive) {
                model.forgetExistingAccount()
                dismissedExistingAccount = true
            }
            Button("cancel", role: .cancel) { dismissedExistingAccount = true }
        } message: {
            LText("login_existing_account_message", model.existingAccountEmail ?? "")
        }
        .alert("login_forgot_password_dialog_title", isPresented: $showingForgotPassword) {
            TextField("email", text: $forgotPasswordEmail)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
            Button("login_forgot_password_dialog_action") {
                model.forgotPassword(email: forgotPasswordEmail) { forgotPasswordSent = true }
            }
            Button("cancel", role: .cancel) {}
        } message: {
            Text("login_forgot_password_dialog_message")
        }
        .alert("login_forgot_password_success_title", isPresented: $forgotPasswordSent) {
            Button("close") {}
        }
        .sheet(isPresented: .init(get: { model.browserCode != nil }, set: { if !$0 { model.cancelBrowserLogin() } })) {
            BrowserLoginSheet(code: model.browserCode ?? "", onCancel: model.cancelBrowserLogin)
                .presentationDetents([.medium])
        }
    }

    // MARK: Sign in

    private var loginForm: some View {
        VStack(spacing: 16) {
            Text("login_title_neut")
                .font(.title.weight(.semibold))
                .multilineTextAlignment(.center)
                .padding(.bottom, 8)

            if model.canSignInWithBrowser {
                Button(action: model.signInWithBrowser) {
                    Label("login_browser_action", systemImage: "person.badge.key.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .controlSize(.large)
                Text("login_or_type").font(.footnote).foregroundStyle(.secondary)
            }
            if model.canUseSavedCredentials {
                Button(action: model.signInWithSavedCredential) {
                    Label("login_saved_credential_action", systemImage: "person.badge.key.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .controlSize(.large)
                Text("login_or_type").font(.footnote).foregroundStyle(.secondary)
            }

            VStack(spacing: 12) {
                TextField("email", text: $email)
                    .textContentType(.username)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.next)
                    .focused($focus, equals: .email)
                    .onSubmit { focus = .password }
                    .textFieldStyle(.roundedBorder)

                SecureFieldWithReveal(titleKey: "password", text: $password)
                    .textContentType(.password)
                    .submitLabel(.go)
                    .focused($focus, equals: .password)
                    .onSubmit(submitLogin)
            }

            HStack {
                Button("login_forgot_password") {
                    forgotPasswordEmail = email
                    showingForgotPassword = true
                }
                .buttonStyle(.borderless)
                Spacer()
                Button("register_action") {
                    model.clearError()
                    isRegistering = true
                }
                .buttonStyle(.bordered)
                Button("login_action", action: submitLogin)
                    .buttonStyle(.borderedProminent)
                    .disabled(!canSubmitLogin)
            }
            .disabled(model.isLoading)
        }
    }

    private var canSubmitLogin: Bool {
        !email.trimmingCharacters(in: .whitespaces).isEmpty && !password.isEmpty
    }

    private func submitLogin() {
        guard canSubmitLogin else { return }
        model.login(email: email, password: password)
    }

    private func showLogin() {
        model.clearError()
        isRegistering = false
    }
}

// MARK: - Registering

private struct RegisterForm: View {
    let model: AuthModel
    @Binding var email: String
    let onShowLogin: () -> Void

    @State private var code = ""
    @State private var password = ""
    @State private var passwordConfirm = ""
    @State private var usePassword = false

    var body: some View {
        VStack(spacing: 16) {
            Text("register_title")
                .font(.title.weight(.semibold))
                .multilineTextAlignment(.center)
                .padding(.bottom, 8)

            switch model.registrationStep {
            case .email: emailStep
            case .code(let email): codeStep(email: email)
            case .method(let email, _): methodStep(email: email)
            }
        }
        .onChange(of: model.registrationStep) { _, step in
            if case .method = step { usePassword = !model.canRegisterWithPasskey }
        }
    }

    private var emailStep: some View {
        VStack(spacing: 12) {
            Text("register_email_message").frame(maxWidth: .infinity, alignment: .leading)
            TextField("email", text: $email)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.continue)
                .onSubmit(requestCode)
                .textFieldStyle(.roundedBorder)
            buttons(canContinue: !email.trimmingCharacters(in: .whitespaces).isEmpty, onContinue: requestCode)
        }
    }

    private func codeStep(email: String) -> some View {
        VStack(spacing: 12) {
            LText("register_code_message", email).frame(maxWidth: .infinity, alignment: .leading)
            TextField("register_code", text: $code)
                .textContentType(.oneTimeCode)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
                .onSubmit(enterCode)
            Button("register_code_resend") { model.requestRegistrationCode(email: email) }
                .buttonStyle(.borderless)
            buttons(
                canContinue: code.trimmingCharacters(in: .whitespaces).count == 6,
                onContinue: enterCode,
                onBack: model.registrationBack
            )
        }
    }

    @ViewBuilder
    private func methodStep(email: String) -> some View {
        if !usePassword {
            VStack(alignment: .leading, spacing: 8) {
                Label("passkey_explanation_title", systemImage: "person.badge.key.fill").font(.headline)
                Text("passkey_explanation_message")
                Button(action: model.registerWithPasskey) {
                    Label("passkey_explanation_action", systemImage: "person.badge.key.fill").frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
                .controlSize(.large)
            }
            .padding()
            .background(.fill.tertiary, in: RoundedRectangle(cornerRadius: 20))

            Button("register_password_instead") { usePassword = true }.buttonStyle(.borderless)
            buttons(canContinue: false, showContinue: false, onBack: model.registrationBack)
        } else {
            VStack(spacing: 12) {
                Text("register_password_message").frame(maxWidth: .infinity, alignment: .leading)
                // Where the password manager takes the account's username from when saving the new password
                TextField("email", text: .constant(email))
                    .textContentType(.username)
                    .disabled(true)
                    .textFieldStyle(.roundedBorder)
                SecureFieldWithReveal(titleKey: "password", text: $password)
                    .textContentType(.newPassword)
                SecureFieldWithReveal(titleKey: "confirm_password", text: $passwordConfirm)
                    .textContentType(.newPassword)
                if model.canRegisterWithPasskey {
                    Button("register_passkey_instead") { usePassword = false }.buttonStyle(.borderless)
                }
                buttons(
                    canContinue: !password.isEmpty && password == passwordConfirm,
                    continueTitle: "register_action",
                    onContinue: { model.register(password: password) },
                    onBack: model.registrationBack
                )
            }
        }
    }

    private func buttons(
        canContinue: Bool,
        showContinue: Bool = true,
        continueTitle: LocalizedStringKey = "register_continue",
        onContinue: @escaping () -> Void = {},
        onBack: (() -> Void)? = nil
    ) -> some View {
        HStack {
            if let onBack {
                Button("register_back", action: onBack).buttonStyle(.borderless)
            }
            Spacer()
            Button("login_action", action: onShowLogin).buttonStyle(.bordered)
            if showContinue {
                Button(continueTitle, action: onContinue)
                    .buttonStyle(.borderedProminent)
                    .disabled(!canContinue)
            }
        }
        .disabled(model.isLoading)
    }

    private func requestCode() {
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty { model.requestRegistrationCode(email: trimmed) }
    }

    private func enterCode() { model.enterRegistrationCode(code) }
}

// MARK: - Pieces

private struct SecureFieldWithReveal: View {
    let titleKey: LocalizedStringKey
    @Binding var text: String
    @State private var isShown = false

    var body: some View {
        HStack {
            Group {
                if isShown { TextField(titleKey, text: $text) } else { SecureField(titleKey, text: $text) }
            }
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()

            Button { isShown.toggle() } label: {
                Image(systemName: isShown ? "eye.slash" : "eye")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(isShown ? "password_hide" : "password_show")
        }
        .padding(8)
        .background(.fill.quaternary, in: RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(.separator))
    }
}

private struct ErrorCard: View {
    let error: AuthError
    let passkeysSupported: Bool

    var body: some View {
        Label {
            message
        } icon: {
            Image(systemName: "exclamationmark.circle.fill")
        }
        .font(.callout.weight(.medium))
        .foregroundStyle(.red)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.red.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder private var message: some View {
        switch error.kind {
        case .userNotRegistered: Text("login_error_user_not_registered")
        case .invalidCredentials: Text("login_error_invalid_credentials")
        case .invalidCode: Text("register_error_invalid_code")
        case .emailNotFound: Text("register_error_email_not_found")
        case .memberNotActive: Text("register_error_member_not_active")
        case .alreadyRegistered: Text("register_error_already_registered")
        case .passwordNotSafe: Text("register_error_password_not_safe")
        case .passwordNotSet: Text(passkeysSupported ? "login_error_password_not_set" : "login_error_password_not_set_desktop")
        case .browserExpired: Text("login_error_browser_expired")
        case .passkey: LText("login_error_passkey", error.detail ?? "")
        case .unknown: LText("login_error_unknown", error.detail ?? "")
        }
    }
}

private struct BrowserLoginSheet: View {
    let code: String
    let onCancel: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("login_browser_title").font(.title2.weight(.semibold))
            Text("login_browser_message").multilineTextAlignment(.center)
            Text("login_browser_code").font(.footnote).foregroundStyle(.secondary)
            Text(code).font(.system(.largeTitle, design: .monospaced).weight(.bold))
            ProgressView()
            Button("cancel", action: onCancel).buttonStyle(.bordered)
        }
        .padding(24)
    }
}
