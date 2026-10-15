import ComposeApp
import SwiftUI

/// An image the shared code downloads (and keeps on the device): the placeholder shows until it is there, or stays
/// if there is none.
struct KotlinImage: View {
    let key: AnyHashable
    let load: () async -> Data?
    var contentMode: ContentMode = .fit

    @State private var image: UIImage?
    @State private var failed = false

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().aspectRatio(contentMode: contentMode)
            } else if failed {
                Image(systemName: "photo").foregroundStyle(.secondary)
            } else {
                ProgressView()
            }
        }
        .task(id: key) {
            image = nil
            failed = false
            if let data = await load(), let loaded = UIImage(data: data) {
                image = loaded
            } else {
                failed = true
            }
        }
    }
}
