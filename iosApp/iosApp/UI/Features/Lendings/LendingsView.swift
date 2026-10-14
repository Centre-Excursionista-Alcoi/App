import ComposeApp
import SwiftUI

/// Native counterpart of `LendingsPage.kt`: the gear of the club by department, what the user picked, and the way to
/// borrow it. The spaces (see `ComposeSpacesIsland`) are still Compose.
struct LendingsView: View {
    @Environment(ShellModel.self) private var shell
    @State private var model = LendingsModel()
    @State private var selectedSection: String?
    @State private var selectedCategories: Set<String> = []
    @State private var showingSpaces = false
    @State private var showingHistory = false

    private let columns = [GridItem(.adaptive(minimum: 170), spacing: 12, alignment: .top)]

    var body: some View {
        VStack(spacing: 0) {
            if model.hasSpaces {
                Picker("lending_mode", selection: $showingSpaces) {
                    Text("nav_lendings").tag(false)
                    Text("spaces_tab").tag(true)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
                .padding(.bottom, 8)
            }

            if showingSpaces && model.hasSpaces {
                ComposeSpacesIsland(hasItemsSelected: model.selectedCount > 0, onNavigate: shell.push)
            } else {
                gear
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showingHistory = true } label: { Label("lending_details_history", systemImage: "clock.arrow.circlepath") }
            }
        }
        .sheet(isPresented: $showingHistory) { LendingHistoryView(lendings: model.history) { lending in
            showingHistory = false
            shell.push(DestinationLendingDetails(lendingId: lending.id))
        } }
        .safeAreaInset(edge: .bottom) {
            if model.selectedCount > 0 && !showingSpaces {
                Button {
                    shell.push(model.checkoutDestination())
                } label: {
                    Label {
                        LText("shopping_list_selected", model.selectedCount)
                    } icon: {
                        Image(systemName: "list.bullet.clipboard")
                    }
                    .padding(.horizontal, 8)
                }
                .buttonStyle(.glassProminent)
                .controlSize(.large)
                .padding(.bottom, 8)
            }
        }
    }

    @ViewBuilder
    private var gear: some View {
        switch model.availability {
        case .loading:
            ProgressView().frame(maxHeight: .infinity)
        case .notRegistered:
            requirementCard(
                systemImage: "person.text.rectangle",
                title: "lending_signup_required",
                action: "lending_signup_action"
            ) { shell.push(DestinationLendingSignUp.shared) }
        case .noInsurance:
            requirementCard(
                systemImage: "cross.case",
                title: "lending_no_active_insurances_title",
                message: "lending_no_active_insurances_message",
                action: "lending_no_active_insurances_action"
            ) { shell.selectedTab = .profile }
        case .ready:
            catalog
        }
    }

    private var catalog: some View {
        let section = currentSection
        let categories = Array(Set(section?.entries.flatMap { ($0.type.categories ?? []).compactMap { $0 as? String } } ?? [])).sorted()
        let entries = (section?.entries ?? []).filter { entry in
            selectedCategories.isEmpty || !selectedCategories.isDisjoint(with: (entry.type.categories ?? []).compactMap { $0 as? String })
        }

        return ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if model.sections.count > 1 {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach(model.sections) { section in
                                chip(section.department?.displayName ?? String(localized: "lending_category_without_department"),
                                     selected: section.id == currentSection?.id) {
                                    selectedSection = section.id
                                    selectedCategories = []
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                if !categories.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach(categories, id: \.self) { category in
                                chip(category, selected: selectedCategories.contains(category)) {
                                    if !selectedCategories.insert(category).inserted { selectedCategories.remove(category) }
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                }

                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(entries) { entry in
                        GearCard(
                            entry: entry,
                            selected: model.selectedAmount(of: entry.type),
                            onAdd: { model.add(entry.type) },
                            onRemove: { model.remove(entry.type) },
                            onOpen: { shell.push(DestinationItemTypeDetails(typeId: entry.type.id, displayName: entry.type.displayName)) }
                        )
                    }
                }
                .padding(.horizontal)
            }
            .padding(.vertical, 8)
        }
    }

    private var currentSection: LendingsModel.Section? {
        model.sections.first { $0.id == selectedSection } ?? model.sections.first
    }

    private func chip(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).font(.subheadline.weight(.medium))
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(selected ? AnyShapeStyle(.tint) : AnyShapeStyle(.fill.tertiary), in: Capsule())
                .foregroundStyle(selected ? Color.white : Color.primary)
        }
        .buttonStyle(.plain)
    }

    private func requirementCard(
        systemImage: String,
        title: LocalizedStringKey,
        message: LocalizedStringKey? = nil,
        action: LocalizedStringKey,
        onAction: @escaping () -> Void
    ) -> some View {
        InfoCard(systemImage: systemImage, tint: .accentColor, title: title, message: message ?? "") {
            Button(action, action: onAction).buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxHeight: .infinity, alignment: .top)
    }
}

private struct GearCard: View {
    let entry: LendingsModel.CatalogEntry
    let selected: Int
    let onAdd: () -> Void
    let onRemove: () -> Void
    let onOpen: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .bottom) {
                Rectangle().fill(.tint.opacity(0.12))
                KotlinImage(key: AnyHashable(entry.id), load: {
                    (try? await ImageLoading.shared.image(container: entry.type))
                }, contentMode: .fill)
                HStack {
                    if selected > 0 {
                        Text("\(selected)").font(.headline).padding(.horizontal, 12).padding(.vertical, 6)
                            .glassEffect(.regular, in: Capsule())
                    }
                    Spacer()
                    GlassEffectContainer {
                        HStack(spacing: 8) {
                            if selected > 0 {
                                Button(action: onRemove) { Image(systemName: "minus") }
                                    .accessibilityLabel("shopping_list_remove")
                            }
                            Button(action: onAdd) { Image(systemName: "plus") }
                                .disabled(selected >= entry.available)
                                .accessibilityLabel("shopping_list_add")
                        }
                        .buttonStyle(.glass)
                        .controlSize(.small)
                    }
                }
                .padding(8)
            }
            .aspectRatio(1, contentMode: .fit)
            .clipped()

            HStack {
                Text(entry.type.displayName).font(.subheadline.weight(.semibold)).lineLimit(2)
                Spacer(minLength: 4)
                Text("\(entry.available)").font(.caption.weight(.bold))
                    .padding(.horizontal, 8).padding(.vertical, 2)
                    .background(.tint.opacity(0.2), in: Capsule())
            }
            .padding(10)
        }
        .background(.background.secondary, in: RoundedRectangle(cornerRadius: 18))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .contentShape(RoundedRectangle(cornerRadius: 18))
        .onTapGesture(perform: onOpen)
    }
}

/// The user's past lendings.
private struct LendingHistoryView: View {
    let lendings: [ReferencedLending]
    let onOpen: (ReferencedLending) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(lendings, id: \.id) { lending in
                Button { onOpen(lending) } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(lending.from.date ..< max(lending.to.date, lending.from.date), format: .interval.day().month(.wide).year())
                            .foregroundStyle(.primary)
                        Text(Set(lending.items.map(\.type.displayName)).sorted().joined(separator: ", "))
                            .font(.footnote).foregroundStyle(.secondary).lineLimit(2)
                    }
                }
            }
            .overlay { if lendings.isEmpty { ContentUnavailableView("lending_details_history", systemImage: "clock") } }
            .navigationTitle("lending_details_history")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("close") { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
    }
}
