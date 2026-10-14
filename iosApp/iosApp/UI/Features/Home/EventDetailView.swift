import ComposeApp
import EventKit
import EventKitUI
import SwiftUI

/// An event: where and when, what it takes to attend, and signing up. What can be done and why not is decided by
/// the shared `assistanceState`; this words it.
struct EventDetailView: View {
    let model: HomeModel
    let event: ReferencedEvent

    @Environment(\.dismiss) private var dismiss
    @State private var addingToCalendar = false
    @State private var isWorking = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if event.image != nil {
                        KotlinImage(key: AnyHashable(event.id)) { (try? await ImageLoading.shared.image(container: event)) }
                            .frame(maxWidth: .infinity, maxHeight: 280)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        LText("event_by", event.department?.displayName ?? String(localized: "event_department_generic"))
                            .font(.subheadline).foregroundStyle(.secondary)
                        EventFormat.dateRange(start: event.start.date, end: event.end?.date).font(.headline)
                    }

                    Label(event.place, systemImage: "mappin.and.ellipse")

                    Button { addingToCalendar = true } label: {
                        Label("event_add_to_calendar", systemImage: "calendar.badge.plus").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    if let state = model.assistance(for: event) {
                        assistance(state)
                    }

                    if let description = event.description_, !description.isEmpty {
                        Text(Markdown.attributed(description))
                    }
                }
                .padding()
            }
            .navigationTitle(event.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) { Button("close") { dismiss() } }
            }
            .sheet(isPresented: $addingToCalendar) {
                CalendarEventEditor(
                    title: event.title,
                    notes: event.description_,
                    location: event.place,
                    start: event.start.date,
                    end: event.end?.date
                )
                .ignoresSafeArea()
            }
        }
        .presentationDetents([.large])
    }

    @ViewBuilder
    private func assistance(_ state: EventAssistanceState) -> some View {
        switch state.insurance {
        case .notRequired: EmptyView()
        case .none: Text("event_requires_insurance_none").bold().foregroundStyle(.red)
        case .notValidForEvent:
            LText("event_requires_insurance_period", dateRangeString).bold().foregroundStyle(.red)
        case .valid: Text("event_requires_insurance_valid").bold().foregroundStyle(.green)
        }

        if !state.requirements.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Text("event_qualifications_required").bold()
                ForEach(Array(state.requirements.enumerated()), id: \.offset) { _, requirement in
                    Label {
                        Text(requirement.alternatives.map { ($0 as? String) ?? String(localized: "event_qualification_unknown") }
                            .joined(separator: " \(String(localized: "event_qualifications_or")) "))
                    } icon: {
                        Image(systemName: requirement.isMet ? "checkmark.circle.fill" : "xmark.circle")
                    }
                    .foregroundStyle(requirement.isMet ? .green : .red)
                }
            }
        }

        if state.requiresConfirmation {
            VStack(alignment: .leading, spacing: 8) {
                Text("event_requires_confirmation").bold()
                if state.isConfirmed {
                    Button(role: .destructive) {
                        run { model.rejectAssistance(event) }
                    } label: {
                        Text("event_reject_assistance").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .disabled(!state.canChangeAssistance || isWorking)
                } else {
                    Button {
                        run { model.confirmAssistance(event) }
                    } label: {
                        Text("event_confirm_assistance").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.glassProminent)
                    .disabled(!state.canConfirm || isWorking)
                }
                if !state.isUserInDepartment {
                    Text("event_not_part_of_department").foregroundStyle(.red)
                }
                if !state.canChangeAssistance {
                    Text("event_assistance_closed").foregroundStyle(.red)
                }
                if state.showMissingQualifications {
                    Text("event_qualifications_missing").foregroundStyle(.red)
                }
            }
        }
    }

    private var dateRangeString: String {
        let start = event.start.date
        guard let end = event.end?.date, end > start else { return start.formatted(date: .long, time: .shortened) }
        return (start ..< end).formatted(.interval.day().month(.wide).year().hour().minute())
    }

    /// The change takes a moment: the buttons stay disabled meanwhile.
    private func run(_ action: () -> Void) {
        isWorking = true
        action()
        Task {
            try? await Task.sleep(for: .seconds(1.5))
            isWorking = false
        }
    }
}

/// The system's "new event" sheet, filled in: it needs no calendar permission, as the user is the one saving it.
struct CalendarEventEditor: UIViewControllerRepresentable {
    typealias UIViewControllerType = EKEventEditViewController
    typealias Coordinator = CalendarEventEditorCoordinator

    let title: String
    let notes: String?
    let location: String
    let start: Date
    let end: Date?

    func makeCoordinator() -> CalendarEventEditorCoordinator { CalendarEventEditorCoordinator() }

    func makeUIViewController(context: UIViewControllerRepresentableContext<CalendarEventEditor>) -> EKEventEditViewController {
        let store = EKEventStore()
        let event = EKEvent(eventStore: store)
        event.title = title
        event.notes = notes
        event.location = location
        event.startDate = start
        event.endDate = end ?? start.addingTimeInterval(60 * 60)
        event.calendar = store.defaultCalendarForNewEvents

        let controller = EKEventEditViewController()
        controller.eventStore = store
        controller.event = event
        controller.editViewDelegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ controller: EKEventEditViewController, context: UIViewControllerRepresentableContext<CalendarEventEditor>) {}
}

final class CalendarEventEditorCoordinator: NSObject, EKEventEditViewDelegate {
    func eventEditViewController(_ controller: EKEventEditViewController, didCompleteWith action: EKEventEditViewAction) {
        controller.dismiss(animated: true)
    }
}
