import ComposeApp
import Observation

/// Wraps the shared `SettingsViewModel`: all the behaviour is Kotlin's, this only makes it observable by SwiftUI.
@MainActor
@Observable
final class SettingsModel {
    struct LanguageOption: Identifiable, Hashable {
        let code: String
        let displayName: String
        var id: String { code }
    }

    @ObservationIgnored
    private let viewModel: SettingsViewModel
    @ObservationIgnored
    private var tasks: [Task<Void, Never>] = []

    private(set) var privacyErrors: Bool
    private(set) var privacyAnalytics: Bool
    private(set) var privacySessionReplay: Bool
    private(set) var sseConnected: Bool
    private(set) var sseError: String?
    private(set) var fcmToken: String?
    private(set) var languageCode: String?

    let languages: [LanguageOption]
    let isLiveUpdatesEnabled = ViewModelFactory.shared.isLiveUpdatesEnabled

    init(onDeleteAccount: @escaping () -> Void) {
        let viewModel = ViewModelFactory.shared.settingsViewModel(onDeleteAccount: { Task { @MainActor in onDeleteAccount() } })
        self.viewModel = viewModel

        languages = SettingsScreenKt.availableLanguages.map { LanguageOption(code: $0.code, displayName: $0.displayName) }
        privacyErrors = viewModel.privacyErrors.value.boolValue
        privacyAnalytics = viewModel.privacyAnalytics.value.boolValue
        privacySessionReplay = viewModel.privacySessionReplay.value.boolValue
        sseConnected = viewModel.sseConnected.value.boolValue
        sseError = viewModel.sseError.value?.message
        fcmToken = viewModel.fcmToken.value
        languageCode = viewModel.language.value?.code

        tasks = [
            collect(viewModel.privacyErrors) { [weak self] in self?.privacyErrors = $0.boolValue },
            collect(viewModel.privacyAnalytics) { [weak self] in self?.privacyAnalytics = $0.boolValue },
            collect(viewModel.privacySessionReplay) { [weak self] in self?.privacySessionReplay = $0.boolValue },
            collect(viewModel.sseConnected) { [weak self] in self?.sseConnected = $0.boolValue },
            collect(viewModel.sseError) { [weak self] in self?.sseError = $0?.message },
            collect(viewModel.fcmToken) { [weak self] in self?.fcmToken = $0 },
            collect(viewModel.language) { [weak self] in self?.languageCode = $0?.code },
        ]
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }

    func setPrivacyErrors(_ enabled: Bool) { _ = viewModel.onPrivacyErrorsChange(state: enabled) }
    func setPrivacyAnalytics(_ enabled: Bool) { _ = viewModel.onPrivacyAnalyticsChange(state: enabled) }
    func setPrivacySessionReplay(_ enabled: Bool) { _ = viewModel.onPrivacySessionReplayChange(state: enabled) }

    func setLanguage(code: String) {
        guard let language = SettingsScreenKt.languageFromCode(code: code) else { return }
        _ = viewModel.onLanguageChange(language: language)
    }

    func deleteAccount() { _ = viewModel.deleteAccount() }

    func reconnectLiveUpdates() { ViewModelFactory.shared.startListeningForNotifications() }
}
