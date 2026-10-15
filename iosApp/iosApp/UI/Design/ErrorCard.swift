import ComposeApp
import SwiftUI

/// A failure signing in, registering or changing the account's security, worded for the user.
struct ErrorCard: View {
    let error: AuthError
    var passkeysSupported = true

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
        case .reauthenticationFailed: Text("security_error_reauthentication")
        case .lastLoginMethod: Text("security_error_last_login_method")
        case .browserExpired: Text("login_error_browser_expired")
        case .passkey: LText("login_error_passkey", error.detail ?? "")
        case .unknown: LText("login_error_unknown", error.detail ?? "")
        }
    }
}
