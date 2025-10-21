import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        SentryKt.initializeSentry()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}