import ComposeApp
import SwiftUI

/// A post of the club, with its link and pictures.
struct PostDetailView: View {
    let post: ReferencedPost

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var zoomedImage: ZoomedImage?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 4) {
                        LText("post_by", post.department?.displayName ?? String(localized: "post_department_generic"))
                            .font(.subheadline).foregroundStyle(.secondary)
                        Text(post.date.date, format: .dateTime.day().month(.wide).year()).font(.headline)
                    }

                    if let link = post.link, let url = URL(string: link) {
                        Button { openURL(url) } label: {
                            Label(link, systemImage: "link").lineLimit(1)
                        }
                    }

                    Text(Markdown.attributed(post.content))

                    if !post.images.isEmpty {
                        ScrollView(.horizontal) {
                            LazyHStack(spacing: 12) {
                                ForEach(post.images, id: \.self) { id in
                                    KotlinImage(key: AnyHashable(id)) {
                                        (try? await ImageLoading.shared.image(container: post, id: id))
                                    }
                                    .frame(height: 280)
                                    .frame(minWidth: 160)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .onTapGesture { zoomedImage = ZoomedImage(id: id, post: post) }
                                }
                            }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle(post.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) { Button("close") { dismiss() } }
            }
            .fullScreenCover(item: $zoomedImage) { image in
                ZoomedImageView(image: image)
            }
        }
    }
}

struct ZoomedImage: Identifiable {
    let id: KotlinUuid
    let post: ReferencedPost
}

/// A picture on its own, to pinch and pan.
private struct ZoomedImageView: View {
    let image: ZoomedImage
    @Environment(\.dismiss) private var dismiss
    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.black.ignoresSafeArea()
            KotlinImage(key: AnyHashable(image.id)) {
                (try? await ImageLoading.shared.image(container: image.post, id: image.id))
            }
            .scaleEffect(scale)
            .gesture(
                MagnifyGesture()
                    .onChanged { scale = max(1, lastScale * $0.magnification) }
                    .onEnded { _ in lastScale = scale }
            )
            Button { dismiss() } label: {
                Image(systemName: "xmark").padding(12)
            }
            .buttonStyle(.glass)
            .padding()
            .accessibilityLabel("close")
        }
    }
}
