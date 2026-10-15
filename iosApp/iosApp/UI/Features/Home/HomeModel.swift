import ComposeApp
import Observation
import UIKit

/// Wraps the shared `HomePageModel`: the user's profile, the events and posts, and the notification permission.
@MainActor
@Observable
final class HomeModel {
    enum NotificationPrompt { case none, ask, openSettings }

    @ObservationIgnored private let viewModel = ViewModelFactory.shared.homePageModel()
    @ObservationIgnored private var tasks: [Task<Void, Never>] = []

    private(set) var profile: ProfileResponse?
    private(set) var upcomingEvents: [ReferencedEvent] = []
    private(set) var posts: [ReferencedPost] = []
    private(set) var qualifications: [Qualification] = []
    private(set) var myQualificationGrants: [QualificationGrant] = []
    private(set) var notificationPrompt: NotificationPrompt = .none
    private(set) var isLoaded = false

    init() {
        tasks = [
            collect(viewModel.profile) { [weak self] in self?.profile = $0 },
            collect(viewModel.upcomingEvents) { [weak self] in
                guard let events = $0 else { return }
                self?.upcomingEvents = events
                self?.isLoaded = true
            },
            collect(viewModel.posts) { [weak self] in self?.posts = $0 ?? [] },
            collect(viewModel.qualifications) { [weak self] in self?.qualifications = $0 ?? [] },
            collect(viewModel.myQualificationGrants) { [weak self] in self?.myQualificationGrants = $0 ?? [] },
            collect(viewModel.notificationPrompt) { [weak self] prompt in
                switch prompt {
                case .ask: self?.notificationPrompt = .ask
                case .openSettings: self?.notificationPrompt = .openSettings
                default: self?.notificationPrompt = .none
                }
            },
        ]
        refreshPermissions()
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }

    /// Notifications are only asked for from users registered for lendings.
    var isRegisteredForLendings: Bool { profile?.lendingUser != nil }

    func refreshPermissions() { _ = viewModel.refreshPermissions() }
    func requestNotifications() { _ = viewModel.requestNotificationsPermission() }
    func denyNotifications() { _ = viewModel.denyNotificationsPermission() }
    func openNotificationSettings() { viewModel.openNotificationSettings() }

    func assistance(for event: ReferencedEvent) -> EventAssistanceState? {
        guard let profile else { return nil }
        return event.assistanceState(
            profile: profile,
            qualifications: qualifications,
            myQualificationGrants: myQualificationGrants
        )
    }

    func confirmAssistance(_ event: ReferencedEvent) { _ = viewModel.confirmEventAssistance(event: event) }
    func rejectAssistance(_ event: ReferencedEvent) { _ = viewModel.rejectEventAssistance(event: event) }
}
