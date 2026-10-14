import ComposeApp
import Foundation
import Observation

/// Wraps the shared `ProfilePageModel`: the member's card, insurances, FEMECV account and departments.
@MainActor
@Observable
final class ProfileModel {
    enum InsuranceStatus { case active, upcoming, expired }

    struct Insurance: Identifiable {
        let id: String
        let company: String
        let policyNumber: String
        let status: InsuranceStatus
        let date: Date
        let isFemecvSync: Bool
    }

    struct Membership: Identifiable {
        let department: Department
        let confirmed: Bool
        var id: String { department.id.toHexDashString() }
    }

    @ObservationIgnored private let viewModel = ViewModelFactory.shared.profilePageModel()
    @ObservationIgnored private var tasks: [Task<Void, Never>] = []

    private(set) var profile: ProfileResponse?
    private(set) var insurances: [Insurance] = []
    private(set) var hasActiveInsurance = true
    private(set) var memberships: [Membership] = []
    private(set) var joinableDepartments: [Department] = []

    init() {
        tasks = [
            collect(viewModel.profile) { [weak self] in self?.profile = $0 },
            collect(viewModel.insuranceRows) { [weak self] rows in
                self?.insurances = (rows ?? []).map { row in
                    Insurance(
                        id: row.id.toHexDashString(),
                        company: row.company,
                        policyNumber: row.policyNumber,
                        status: row.status == .active ? .active : (row.status == .upcoming ? .upcoming : .expired),
                        date: row.date.date,
                        isFemecvSync: row.isFemecvSync
                    )
                }
            },
            collect(viewModel.hasActiveInsurance) { [weak self] in self?.hasActiveInsurance = $0?.boolValue ?? true },
            collect(viewModel.memberships) { [weak self] rows in
                self?.memberships = (rows ?? []).map { Membership(department: $0.department, confirmed: $0.confirmed) }
            },
            collect(viewModel.joinableDepartments) { [weak self] in self?.joinableDepartments = $0 ?? [] },
        ]
    }

    deinit {
        tasks.forEach { $0.cancel() }
        ViewModelFactory.shared.dispose(viewModel: viewModel)
    }

    /// Whether the insurance was stored (it isn't if the server refused it).
    func addInsurance(company: String, policyNumber: String, from: Date, to: Date, documents: [URL]) async -> Bool {
        let result = try? await viewModel.addInsurance(
            company: company,
            policyNumber: policyNumber,
            validFrom: Self.isoDay(from),
            validTo: Self.isoDay(to),
            documentPaths: documents.map(\.path)
        )
        return result?.boolValue ?? false
    }

    /// Why FEMECV said no, or `nil` once the account is connected.
    func connectFemecv(username: String, password: String) async -> String? {
        do {
            return try await viewModel.connectFemecv(username: username, password: password)
        } catch {
            return error.localizedDescription
        }
    }

    func disconnectFemecv() { _ = viewModel.disconnectFEMECV() }
    func join(_ department: Department) { _ = viewModel.requestJoinDepartment(department: department) }
    func leave(_ department: Department) { _ = viewModel.leaveDepartment(department: department) }

    private static func isoDay(_ date: Date) -> String {
        let parts = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year ?? 1970, parts.month ?? 1, parts.day ?? 1)
    }
}
