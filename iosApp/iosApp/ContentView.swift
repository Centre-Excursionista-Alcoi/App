import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    typealias UIViewControllerType = UIViewController
    typealias Coordinator = Void

    func makeUIViewController(context: UIViewControllerRepresentableContext<ComposeView>) -> UIViewControllerType {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewControllerType, context: UIViewControllerRepresentableContext<ComposeView>) {}
}

struct ContentView: View {
    var body: some View {
        content
            .onOpenURL { url in
                // A universal link like https://centrexcursionistalcoi.app/admin/lendings/<id>, from an email or
                // another app. The shared code opens it as soon as the app can, which for a launch from the link
                // is once the user is in.
                DeepLinks.shared.receive(url: url.absoluteString)
            }
    }
}

extension ContentView {
    @ViewBuilder
    fileprivate var content: some View {
        #if DEBUG
        // Shows the native screens under construction instead of the Compose app: pass `-CEANativeSettings`.
        if ProcessInfo.processInfo.arguments.contains("-CEANativeSettings") {
            NavigationStack { SettingsView() }
        } else {
            ComposeView().ignoresSafeArea()
        }
        #else
        ComposeView().ignoresSafeArea()
        #endif
    }
}
