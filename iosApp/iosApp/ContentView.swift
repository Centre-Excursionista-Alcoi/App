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
        ComposeView()
            .ignoresSafeArea()
            .onOpenURL { url in
                // A link like cea://admin/lendings#<id>, from an email or another app. The shared code opens it
                // as soon as the app can, which for a launch from the link is once the user is in.
                DeepLinks.shared.receive(url: url.absoluteString)
            }
    }
}
