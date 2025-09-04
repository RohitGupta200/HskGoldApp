import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        print("[CapGold] ComposeView.makeUIViewController() start")
        let vc = MainViewControllerKt.MainViewController()
        print("[CapGold] ComposeView.makeUIViewController() got VC: \(type(of: vc))")
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .onAppear { print("[CapGold] ContentView.onAppear") }
    }
}



