import ComposeApp
import Foundation
import Observation

/// Wraps the shared `LendingsPageModel` (and `SpacesViewModel`, for the spaces tab): the gear the user can borrow,
/// what they picked so far, and their lendings of the past.
@MainActor
@Observable
final class LendingsModel {
    enum Availability { case loading, notRegistered, noInsurance, ready }

    struct Section: Identifiable {
        let department: Department?
        let entries: [CatalogEntry]
        var id: String { department?.id.toHexDashString() ?? "none" }
    }

    struct CatalogEntry: Identifiable {
        let type: ReferencedInventoryItemType
        let available: Int
        var id: String { type.id.toHexDashString() }
    }

    @ObservationIgnored private let viewModel = ViewModelFactory.shared.lendingsPageModel()
    @ObservationIgnored private let spaces = ViewModelFactory.shared.spacesViewModel()
    @ObservationIgnored private var tasks: [Task<Void, Never>] = []

    private(set) var availability: Availability = .loading
    private(set) var sections: [Section] = []
    private(set) var selectedAmounts: [String: Int] = [:]
    private(set) var selectedCount = 0
    private(set) var history: [ReferencedLending] = []
    private(set) var hasSpaces = false

    init() {
        tasks = [
            collect(viewModel.availability) { [weak self] availability in
                switch availability {
                case .notRegistered: self?.availability = .notRegistered
                case .noInsurance: self?.availability = .noInsurance
                case .ready: self?.availability = .ready
                default: self?.availability = .loading
                }
            },
            collect(viewModel.catalog) { [weak self] sections in
                self?.sections = (sections ?? []).map { section in
                    Section(
                        department: section.department,
                        entries: section.entries.map { CatalogEntry(type: $0.type, available: Int($0.available)) }
                    )
                }
            },
            collect(viewModel.selectedAmounts) { [weak self] amounts in
                var result: [String: Int] = [:]
                for (key, value) in amounts { result[key] = value.intValue }
                self?.selectedAmounts = result
            },
            collect(viewModel.selectedCount) { [weak self] in self?.selectedCount = $0.intValue },
            collect(viewModel.history) { [weak self] in self?.history = $0 ?? [] },
            collect(spaces.hasSpaces) { [weak self] in self?.hasSpaces = $0.boolValue },
        ]
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: viewModel)
        ViewModelFactory.shared.dispose(viewModel: spaces)
    }

    func selectedAmount(of type: ReferencedInventoryItemType) -> Int {
        selectedAmounts[type.id.toString()] ?? 0
    }

    func add(_ type: ReferencedInventoryItemType) { viewModel.addItemToShoppingList(type: type) }
    func remove(_ type: ReferencedInventoryItemType) { viewModel.removeItemFromShoppingList(type: type) }

    /// The screen that confirms borrowing what was picked.
    func checkoutDestination() -> any Destination { viewModel.checkoutDestination() }
}
