import SwiftUI

/// Native counterpart of `SettingsScreen.kt`.
struct SettingsView: View {
    let onDeleteAccount: () -> Void
    let onSecurity: () -> Void

    @State private var model: SettingsModel
    @State private var confirmingDelete = false
    @Environment(\.openURL) private var openURL

    init(onDeleteAccount: @escaping () -> Void, onSecurity: @escaping () -> Void) {
        self.onDeleteAccount = onDeleteAccount
        self.onSecurity = onSecurity
        _model = State(initialValue: SettingsModel(onDeleteAccount: onDeleteAccount))
    }

    var body: some View {
        Form {
            Section("settings_category_general") {
                Picker(selection: languageSelection) {
                    ForEach(model.languages) { language in
                        Label {
                            Text(language.displayName)
                        } icon: {
                            Image("flag_\(language.code)", bundle: nil)
                        }
                        .tag(language.code)
                    }
                } label: {
                    Label("settings_language", systemImage: "globe")
                }
            }

            Section("settings_category_push") {
                LabeledContent {
                    EmptyView()
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Label("settings_push_fcm_connection_title", systemImage: "icloud.and.arrow.down")
                        if let token = model.fcmToken {
                            LText("settings_push_fcm_connection_connected", token)
                                .font(.footnote).foregroundStyle(.secondary).textSelection(.enabled)
                        } else {
                            Text("settings_push_fcm_connection_disconnected")
                                .font(.footnote).foregroundStyle(.secondary)
                        }
                    }
                }

                if model.isLiveUpdatesEnabled {
                    Button(action: model.reconnectLiveUpdates) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("settings_push_connection_title").foregroundStyle(.primary)
                            Group {
                                Text(model.sseConnected ? "settings_push_connection_message_connected" : "settings_push_connection_message_disconnected")
                                if let error = model.sseError { Text(error) }
                            }
                            .font(.footnote).foregroundStyle(.secondary)
                        }
                    }
                    .disabled(model.sseError == nil)
                }
            }

            Section("settings_category_privacy") {
                Button(action: onSecurity) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Label("security_title", systemImage: "lock.shield")
                            Text("security_summary").font(.footnote).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
                    }
                }
                .foregroundStyle(.primary)

                switchRow("settings_report_errors_title", "settings_report_errors_summary", model.privacyErrors, model.setPrivacyErrors)
                switchRow("settings_report_analytics_title", "settings_report_analytics_summary", model.privacyAnalytics, model.setPrivacyAnalytics)
                switchRow("settings_report_session_title", "settings_report_session_summary", model.privacySessionReplay, model.setPrivacySessionReplay)
            }

            Section {
                Button(role: .destructive) { confirmingDelete = true } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Label("settings_remove_account_title", systemImage: "exclamationmark.triangle")
                        Text("settings_remove_account_summary").font(.footnote)
                    }
                }
            }

            Section {
                HStack(spacing: 12) {
                    Image("arnyminerz", bundle: nil).resizable().scaledToFit().frame(width: 56, height: 56)
                    VStack(alignment: .leading, spacing: 4) {
                        LText("settings_dev_credit_title", "Arnau Mora").font(.headline)
                        LText("settings_dev_credit_message", "Arnau Mora Gras").font(.subheadline)
                    }
                }
                HStack {
                    Button { openURL(URL(string: "https://arnyminerz.com")!) } label: {
                        Label("settings_dev_credit_website", systemImage: "globe")
                    }
                    Spacer()
                    Button { openURL(URL(string: "mailto:arnyminerz@proton.me")!) } label: {
                        Label("settings_dev_credit_email", systemImage: "envelope")
                    }
                }
                .buttonStyle(.bordered)
            }
        }
        .navigationTitle("settings_title")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog("settings_remove_account_title", isPresented: $confirmingDelete, titleVisibility: .visible) {
            Button("confirm", role: .destructive, action: model.deleteAccount)
            Button("cancel", role: .cancel) {}
        } message: {
            Text("settings_remove_account_message")
        }
    }

    private var languageSelection: Binding<String> {
        Binding(get: { model.languageCode ?? "en" }, set: { model.setLanguage(code: $0) })
    }

    private func switchRow(
        _ title: LocalizedStringKey,
        _ summary: LocalizedStringKey,
        _ value: Bool,
        _ set: @escaping (Bool) -> Void
    ) -> some View {
        Toggle(isOn: Binding(get: { value }, set: set)) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                Text(summary).font(.footnote).foregroundStyle(.secondary)
            }
        }
    }
}
