import ComposeApp
import SwiftUI

/// The screen the user lands on once they are in: the tabs, and the screens that open over them.
///
/// The tab bar adapts by itself: along the bottom on compact widths (iPhone, a folded foldable) and a sidebar on
/// regular ones (iPad, an unfolded foldable, a resized window). Which tabs there are comes from the shared
/// `MainScreenViewModel`, the same rules the Compose app follows.
struct MainView: View {
    @Environment(ShellModel.self) private var shell
    @State private var model = MainModel()

    var body: some View {
        @Bindable var shell = shell

        TabView(selection: $shell.selectedTab) {
            ForEach(model.tabs, id: \.self) { tab in
                Tab(tab.title, systemImage: tab.systemImage, value: tab) {
                    TabContent(tab: tab, model: model)
                }
                .badge(tab == .management ? model.managementBadge : 0)
            }
        }
        .tabViewStyle(.sidebarAdaptable)
        .onAppear(perform: model.syncIfRequestedByDebugLaunch)
    }
}

private struct TabContent: View {
    @Environment(ShellModel.self) private var shell
    let tab: MainTab
    let model: MainModel

    @State private var confirmingLogout = false

    var body: some View {
        @Bindable var shell = shell

        // Every tab has its own stack, but the screens pushed over them share one path: while one is open the tab
        // bar is hidden, so the user can't switch tabs under it.
        NavigationStack(path: $shell.path) {
            content
            .ignoresSafeArea(.container, edges: .bottom)
            .navigationTitle(tab.title)
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: Route.self) { RouteView(route: $0) }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if model.isAdmin {
                        Text("admin")
                            .font(.caption.weight(.semibold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(.tint, in: Capsule())
                            .foregroundStyle(.white)
                    }
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button(action: model.sync) {
                        if model.isSyncing {
                            ProgressView()
                        } else {
                            Label("force_sync", systemImage: "arrow.triangle.2.circlepath")
                        }
                    }
                    .disabled(model.isSyncing)

                    if tab == .profile {
                        Button { shell.push(DestinationSettings.shared) } label: {
                            Label("settings", systemImage: "gearshape")
                        }
                        Button { confirmingLogout = true } label: {
                            Label("logout", systemImage: "rectangle.portrait.and.arrow.right")
                        }
                    }
                }
            }
        }
        .confirmationDialog("logout_confirmation_title", isPresented: $confirmingLogout, titleVisibility: .visible) {
            Button("logout", role: .destructive, action: shell.requestLogout)
            Button("cancel", role: .cancel) {}
        } message: {
            Text("logout_confirmation_message")
        }
    }
}

extension TabContent {
    @ViewBuilder
    fileprivate var content: some View {
        switch tab {
        case .home:
            HomeView()
        case .profile:
            ProfileView()
        default:
            ComposeTabIsland(
                tab: tab,
                scrollToFemecvAccount: shell.scrollToFemecvAccount && tab == .profile,
                onNavigate: shell.push,
                onTabRequested: { shell.selectedTab = $0 }
            )
        }
    }
}

extension MainTab {
    var title: LocalizedStringKey {
        switch self {
        case .home: "nav_home"
        case .lendings: "nav_lendings"
        case .lending: "nav_lending"
        case .activities: "nav_activities"
        case .management: "nav_management"
        case .profile: "nav_profile"
        }
    }

    var systemImage: String {
        switch self {
        case .home: "house"
        case .lendings: "shippingbox"
        case .lending: "shippingbox.fill"
        case .activities: "figure.hiking"
        case .management: "person.2.badge.gearshape"
        case .profile: "person.crop.circle"
        }
    }
}
