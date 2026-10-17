import ComposeApp
import Observation

/// Wraps the shared `SettingsViewModel`: all the behaviour is Kotlin's, this only makes it observable by SwiftUI.
@MainActor
@Observable
final class SettingsModel {
    @ObservationIgnored
    private let viewModel: SettingsViewModel

    let privacyErrors: FlowState<Bool>
    let privacyAnalytics: FlowState<Bool>
    let privacySessionReplay: FlowState<Bool>
    let sseConnected: FlowState<Bool>
    let languageCode: FlowState<String?>

    init(onDeleteAccount: @escaping () -> Void) {
        let viewModel = ViewModelFactory.shared.settingsViewModel(onDeleteAccount: onDeleteAccount)
        self.viewModel = viewModel

        privacyErrors = FlowState(viewModel.privacyErrors, initial: viewModel.privacyErrors.value.boolValue) { $0.boolValue }
        privacyAnalytics = FlowState(viewModel.privacyAnalytics, initial: viewModel.privacyAnalytics.value.boolValue) { $0.boolValue }
        privacySessionReplay = FlowState(viewModel.privacySessionReplay, initial: viewModel.privacySessionReplay.value.boolValue) { $0.boolValue }
        sseConnected = FlowState(viewModel.sseConnected, initial: viewModel.sseConnected.value.boolValue) { $0.boolValue }
        languageCode = FlowState(viewModel.language, initial: viewModel.language.value?.code) { $0?.code }
    }

    deinit {
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }

    func setPrivacyErrors(_ enabled: Bool) { _ = viewModel.onPrivacyErrorsChange(state: enabled) }
    func setPrivacyAnalytics(_ enabled: Bool) { _ = viewModel.onPrivacyAnalyticsChange(state: enabled) }
    func setPrivacySessionReplay(_ enabled: Bool) { _ = viewModel.onPrivacySessionReplayChange(state: enabled) }

    func setLanguage(code: String) {
        guard let language = SettingsScreenKt.languageFromCode(code: code) else { return }
        _ = viewModel.onLanguageChange(language: language)
    }
}
