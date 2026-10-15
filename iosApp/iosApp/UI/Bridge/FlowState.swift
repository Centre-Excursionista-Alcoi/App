import Observation

/// Keeps the latest value of a Kotlin `Flow` (through SKIE, an `AsyncSequence`) where SwiftUI can observe it.
///
/// The collection runs for as long as this object lives, so the screen's model owns one per flow it shows.
@MainActor
@Observable
final class FlowState<Value> {
    private(set) var value: Value

    @ObservationIgnored
    private var task: Task<Void, Never>?

    init<Flow: AsyncSequence>(
        _ flow: Flow,
        initial: Value,
        transform: @escaping (Flow.Element) -> Value
    ) {
        value = initial
        task = Task { [weak self] in
            do {
                for try await element in flow {
                    self?.value = transform(element)
                }
            } catch {
                // A Kotlin flow that fails has already reported it through the ViewModel's error state.
            }
        }
    }

    deinit {
        task?.cancel()
    }
}

/// Runs [apply] for every value a Kotlin `Flow` emits, until the returned task is cancelled (models do that in
/// `deinit`).
@MainActor
func collect<Flow: AsyncSequence>(
    _ flow: Flow,
    _ apply: @escaping @MainActor (Flow.Element) -> Void
) -> Task<Void, Never> {
    Task {
        do {
            for try await element in flow { apply(element) }
        } catch {
            // A Kotlin flow that fails has already reported it through the ViewModel's error state.
        }
    }
}
