import SwiftUI

/// Native counterpart of `SecurityScreen.kt`: the user's passkeys and password.
struct SecurityView: View {
    @State private var model = SecurityModel()
    @State private var settingPassword = false
    @State private var newPassword = ""
    @State private var newPasswordConfirm = ""
    @State private var confirmationPassword = ""

    var body: some View {
        List {
            if let error = model.error {
                ErrorCard(error: error, passkeysSupported: model.isPasskeysSupported)
                    .listRowInsets(.init())
                    .listRowBackground(Color.clear)
            }

            if model.isLoaded {
                Section("security_passkeys_category") {
                    if model.passkeys.isEmpty && model.isPasskeysSupported {
                        VStack(alignment: .leading, spacing: 8) {
                            Label("passkey_explanation_title", systemImage: "person.badge.key.fill").font(.headline)
                            Text("passkey_explanation_message").font(.subheadline)
                            Button("passkey_explanation_action", action: model.addPasskey)
                                .buttonStyle(.glassProminent)
                        }
                    }
                    ForEach(model.passkeys) { passkey in
                        passkeyRow(passkey)
                    }
                    if !model.passkeys.isEmpty && model.isPasskeysSupported {
                        Button("security_passkey_add", action: model.addPasskey)
                    }
                    if !model.isPasskeysSupported {
                        Text("security_passkeys_unsupported").font(.footnote).foregroundStyle(.secondary)
                    }
                }

                Section("security_password_category") {
                    Button {
                        newPassword = ""
                        newPasswordConfirm = ""
                        settingPassword = true
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(model.hasPassword ? "security_password_change" : "security_password_set")
                            Text(model.hasPassword ? "security_password_set_summary" : "security_password_not_set_summary")
                                .font(.footnote).foregroundStyle(.secondary)
                        }
                    }
                    if model.hasPassword && !model.passkeys.isEmpty {
                        Button(role: .destructive) { model.request(.removePassword) } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("security_password_remove")
                                Text("security_password_remove_summary").font(.footnote)
                            }
                        }
                    }
                }
            }
        }
        .overlay { if !model.isLoaded { ProgressView() } }
        .disabled(model.isLoading)
        .navigationTitle("security_title")
        .navigationBarTitleDisplayMode(.inline)
        .alert("security_password_set", isPresented: $settingPassword) {
            SecureField("security_new_password", text: $newPassword).textContentType(.newPassword)
            SecureField("confirm_password", text: $newPasswordConfirm).textContentType(.newPassword)
            Button("security_password_set") {
                if !newPassword.isEmpty && newPassword == newPasswordConfirm { model.request(.setPassword(newPassword)) }
            }
            Button("cancel", role: .cancel) {}
        } message: {
            Text("register_error_password_not_safe")
        }
        .alert("security_confirm_title", isPresented: confirming) {
            if model.canConfirmWithPasskey {
                Button("security_confirm_passkey", action: model.confirmWithPasskey)
            }
            if model.hasPassword {
                SecureField("password", text: $confirmationPassword).textContentType(.password)
                Button("security_confirm_password") {
                    model.confirmWithPassword(confirmationPassword)
                    confirmationPassword = ""
                }
            }
            Button("cancel", role: .cancel, action: model.cancelChange)
        } message: {
            Text("security_confirm_message")
        }
    }

    private var confirming: Binding<Bool> {
        Binding(get: { model.pendingChange != nil }, set: { if !$0 { model.cancelChange() } })
    }

    private func passkeyRow(_ passkey: SecurityModel.Passkey) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                if let name = passkey.name { Text(name) } else { Text("security_passkey_default_name") }
                Group {
                    LText("security_passkey_created", passkey.createdAt.formatted(date: .abbreviated, time: .omitted))
                    if let lastUsed = passkey.lastUsedAt {
                        LText("security_passkey_last_used", lastUsed.formatted(date: .abbreviated, time: .omitted))
                    } else {
                        Text("security_passkey_never_used")
                    }
                }
                .font(.footnote).foregroundStyle(.secondary)
            }
            Spacer()
            Button(role: .destructive) { model.request(.removePasskey(id: passkey.id)) } label: {
                Image(systemName: "trash")
            }
            .buttonStyle(.borderless)
            .accessibilityLabel("security_passkey_remove")
        }
    }
}
