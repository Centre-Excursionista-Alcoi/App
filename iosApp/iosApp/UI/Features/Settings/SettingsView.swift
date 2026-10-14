import SwiftUI

/// Native counterpart of `SettingsScreen.kt`. A first, partial port: language, privacy and the notification link.
struct SettingsView: View {
    @State private var model = SettingsModel(onDeleteAccount: {})

    // TODO: the list lives in the Compose file; move it to a plain Kotlin list the ViewModel exposes.
    private let languages = [("en", "English"), ("ca", "Català"), ("es", "Castellano")]

    var body: some View {
        Form {
            Section("Language") {
                Picker("Language", selection: languageSelection) {
                    ForEach(languages, id: \.0) { code, name in
                        Text(name).tag(code)
                    }
                }
            }

            Section("Privacy") {
                Toggle("Error reports", isOn: binding(model.privacyErrors, model.setPrivacyErrors))
                Toggle("Analytics", isOn: binding(model.privacyAnalytics, model.setPrivacyAnalytics))
                Toggle("Session replay", isOn: binding(model.privacySessionReplay, model.setPrivacySessionReplay))
            }

            Section("Notifications") {
                LabeledContent("Live updates", value: model.sseConnected.value ? "Connected" : "Disconnected")
            }
        }
        .navigationTitle("Settings")
    }

    private var languageSelection: Binding<String> {
        Binding(
            get: { model.languageCode.value ?? "en" },
            set: { model.setLanguage(code: $0) }
        )
    }

    private func binding(_ state: FlowState<Bool>, _ set: @escaping (Bool) -> Void) -> Binding<Bool> {
        Binding(get: { state.value }, set: set)
    }
}
