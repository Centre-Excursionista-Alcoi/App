import ComposeApp
import SwiftUI

/// Native counterpart of `HomePage.kt`: what's coming up, and the club's posts.
struct HomeView: View {
    @Environment(ShellModel.self) private var shell
    @Environment(\.scenePhase) private var scenePhase
    @State private var model = HomeModel()
    @State private var openEvent: ReferencedEvent?
    @State private var openPost: ReferencedPost?

    private let columns = [GridItem(.adaptive(minimum: 320), spacing: 16, alignment: .top)]

    var body: some View {
        Group {
            if let profile = model.profile, model.isLoaded {
                ScrollView {
                    LazyVGrid(columns: columns, alignment: .leading, spacing: 16) {
                        if profile.femecvCredentialsInvalid {
                            femecvWarning.gridCellColumns(1)
                        }
                        if model.isRegisteredForLendings, model.notificationPrompt != .none {
                            notificationCard
                        }
                    }
                    .padding(.horizontal)

                    if model.upcomingEvents.isEmpty && model.posts.isEmpty {
                        ContentUnavailableView("home_empty", systemImage: "tray")
                            .padding(.top, 80)
                    }

                    if !model.upcomingEvents.isEmpty {
                        section("upcoming_events") {
                            ForEach(model.upcomingEvents, id: \.id) { event in
                                FeedCard(
                                    systemImage: "calendar",
                                    title: event.title,
                                    date: EventFormat.dateRange(start: event.start.date, end: event.end?.date),
                                    content: event.description_
                                ) { openEvent = event }
                            }
                        }
                    }

                    if !model.posts.isEmpty {
                        section("posts") {
                            ForEach(model.posts, id: \.id) { post in
                                FeedCard(
                                    systemImage: "newspaper",
                                    title: post.title,
                                    date: Text(post.date.date, format: .dateTime.day().month(.wide).year()),
                                    content: post.content
                                ) { openPost = post }
                            }
                        }
                    }
                }
            } else {
                ProgressView()
            }
        }
        .sheet(item: $openEvent) { event in
            EventDetailView(model: model, event: event)
        }
        .sheet(item: $openPost) { post in
            PostDetailView(post: post)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { model.refreshPermissions() }
        }
    }

    private func section<Content: View>(_ title: LocalizedStringKey, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.title2.weight(.semibold)).padding(.horizontal)
            LazyVGrid(columns: columns, alignment: .leading, spacing: 16) { content() }
                .padding(.horizontal)
        }
        .padding(.top, 16)
        .padding(.bottom, 8)
    }

    private var femecvWarning: some View {
        InfoCard(systemImage: "exclamationmark.triangle.fill", tint: .red, title: "femecv_credentials_invalid_title", message: "femecv_credentials_invalid_message") {
            Button("femecv_credentials_invalid_action") {
                shell.scrollToFemecvAccount = true
                shell.selectedTab = .profile
            }
            .buttonStyle(.bordered)
        }
    }

    private var notificationCard: some View {
        InfoCard(systemImage: "bell.badge", tint: .accentColor, title: "permission_notification_title", message: "permission_notification_message") {
            HStack {
                Button("permission_deny", action: model.denyNotifications).buttonStyle(.bordered)
                if model.notificationPrompt == .openSettings {
                    Button("permission_settings", action: model.openNotificationSettings).buttonStyle(.borderedProminent)
                } else {
                    Button("permission_grant", action: model.requestNotifications).buttonStyle(.borderedProminent)
                }
            }
        }
    }
}

// MARK: - Cards

struct FeedCard: View {
    let systemImage: String
    let title: String
    let date: Text
    let content: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 12) {
                    Image(systemName: systemImage).font(.title3).foregroundStyle(.tint).frame(width: 28)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title).font(.headline).multilineTextAlignment(.leading)
                        date.font(.subheadline).foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                }
                if let content, !content.isEmpty {
                    Text(Markdown.attributed(content))
                        .font(.subheadline)
                        .lineLimit(3)
                        .multilineTextAlignment(.leading)
                        .foregroundStyle(.primary)
                }
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.background.secondary, in: RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
    }
}

struct InfoCard<Actions: View>: View {
    let systemImage: String
    let tint: Color
    let title: LocalizedStringKey
    let message: LocalizedStringKey
    @ViewBuilder let actions: () -> Actions

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(title, systemImage: systemImage).font(.headline).foregroundStyle(tint)
            Text(message).font(.subheadline)
            actions()
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 20))
    }
}

// MARK: - Formatting

enum EventFormat {
    static func dateRange(start: Date, end: Date?) -> Text {
        guard let end, end > start else { return Text(start, format: .dateTime.day().month(.wide).year().hour().minute()) }
        return Text(start ..< end, format: .interval.day().month(.wide).year().hour().minute())
    }
}

enum Markdown {
    /// Kotlin sends Markdown; a single line break starts a new line, as users expect.
    static func attributed(_ markdown: String) -> AttributedString {
        let options = AttributedString.MarkdownParsingOptions(
            interpretedSyntax: .inlineOnlyPreservingWhitespace
        )
        return (try? AttributedString(markdown: markdown, options: options)) ?? AttributedString(markdown)
    }
}

extension ReferencedEvent: @retroactive Identifiable {}
extension ReferencedPost: @retroactive Identifiable {}
