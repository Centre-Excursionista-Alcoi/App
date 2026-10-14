import ComposeApp
import SwiftUI
import UniformTypeIdentifiers

/// Native counterpart of `ProfilePage.kt`: the member card, and what the club knows about the member.
struct ProfileView: View {
    @Environment(ShellModel.self) private var shell
    @State private var model = ProfileModel()
    @State private var addingInsurance = false
    @State private var signingIntoFemecv = false
    @State private var joiningDepartment = false
    @State private var leaving: Department?
    @State private var confirmingFemecvDisconnect = false

    var body: some View {
        if let profile = model.profile {
            ScrollViewReader { proxy in
                List {
                    Section {
                        MemberCard(profile: profile)
                            .listRowInsets(.init())
                            .listRowBackground(Color.clear)
                    }

                    Section("personal_info") {
                        LabeledContent("personal_info_full_name", value: profile.fullName)
                        LabeledContent("personal_info_email", value: profile.email)
                    }

                    if !model.hasActiveInsurance {
                        Section {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("lending_no_active_insurances_title").font(.headline)
                                Text("lending_no_active_insurances_message").font(.subheadline)
                                Button("lending_no_active_insurances_action") { addingInsurance = true }
                            }
                        }
                    }

                    if !model.insurances.isEmpty {
                        Section {
                            ForEach(model.insurances) { InsuranceRow(insurance: $0) }
                        } header: {
                            HStack {
                                Text("insurances_title")
                                Spacer()
                                Button { addingInsurance = true } label: { Image(systemName: "plus") }
                                    .accessibilityLabel("insurance_add_title")
                            }
                        }
                    }

                    Section("femecv_sync_title") { femecvSection(profile) }
                        .id("femecv")

                    Section {
                        if model.memberships.isEmpty {
                            Text("departments_member_none").foregroundStyle(.secondary)
                        }
                        ForEach(model.memberships) { membership in
                            Button { leaving = membership.department } label: {
                                HStack(spacing: 12) {
                                    KotlinImage(key: AnyHashable(membership.id)) {
                                        (try? await ImageLoading.shared.image(container: membership.department))
                                    }
                                    .frame(width: 36, height: 36)
                                    VStack(alignment: .leading) {
                                        Text(membership.department.displayName).foregroundStyle(.primary)
                                        if !membership.confirmed {
                                            Text("departments_member_pending").font(.footnote).foregroundStyle(.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    } header: {
                        HStack {
                            Text("departments_title")
                            Spacer()
                            Button { joiningDepartment = true } label: { Image(systemName: "plus") }
                                .accessibilityLabel("departments_join_choose")
                        }
                    }
                }
                .onAppear { scrollToFemecvIfRequested(proxy) }
                .onChange(of: shell.scrollToFemecvAccount) { _, _ in scrollToFemecvIfRequested(proxy) }
            }
            .sheet(isPresented: $addingInsurance) { AddInsuranceView(model: model) }
            .sheet(isPresented: $signingIntoFemecv) { FemecvLoginView(model: model) }
            .sheet(isPresented: $joiningDepartment) { JoinDepartmentView(model: model) }
            .confirmationDialog(
                "departments_leave_title",
                isPresented: .init(get: { leaving != nil }, set: { if !$0 { leaving = nil } }),
                titleVisibility: .visible,
                presenting: leaving
            ) { department in
                Button("departments_leave", role: .destructive) { model.leave(department) }
                Button("close", role: .cancel) {}
            } message: { department in
                LText("departments_leave_message", department.displayName)
            }
        } else {
            ProgressView()
        }
    }

    @ViewBuilder
    private func femecvSection(_ profile: ProfileResponse) -> some View {
        if profile.femecvSyncEnabled {
            if profile.femecvCredentialsInvalid {
                VStack(alignment: .leading, spacing: 8) {
                    Text("femecv_credentials_invalid_title").font(.headline).foregroundStyle(.red)
                    Text("femecv_credentials_invalid_message").font(.subheadline)
                    Button { signingIntoFemecv = true } label: {
                        Label("femecv_credentials_invalid_action", systemImage: "arrow.triangle.2.circlepath.icloud")
                    }
                    .buttonStyle(.borderedProminent)
                }
            } else {
                Label("femecv_sync_connected", systemImage: "checkmark.icloud")
            }
            Button(role: .destructive) { model.disconnectFemecv() } label: {
                Label("femecv_sync_disable", systemImage: "icloud.slash")
            }
        } else {
            Button { signingIntoFemecv = true } label: {
                Label("femecv_sync_enable", systemImage: "arrow.triangle.2.circlepath.icloud")
            }
        }
    }

    private func scrollToFemecvIfRequested(_ proxy: ScrollViewProxy) {
        guard shell.scrollToFemecvAccount else { return }
        withAnimation { proxy.scrollTo("femecv", anchor: .top) }
        shell.scrollToFemecvAccount = false
    }
}

// MARK: - Pieces

/// The member card, the same colour and layout as the one of the Compose app.
private struct MemberCard: View {
    let profile: ProfileResponse

    var body: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 16).fill(Color(red: 0xA9 / 255, green: 0xA5 / 255, blue: 0x87 / 255))
            Image("icon_monochrome", bundle: nil)
                .resizable().scaledToFit().opacity(0.3)
                .frame(height: 130)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                .padding(16)
            HStack(spacing: 8) {
                Image("forma_pv", bundle: nil).renderingMode(.template).resizable().scaledToFit().frame(height: 40)
                Text("Centre Excursionista\nd'Alcoi").font(.headline)
            }
            .padding(16)
            VStack(alignment: .leading) {
                Spacer()
                Text(profile.fullName).font(.title3.weight(.semibold))
                Text(String(profile.memberNumber)).font(.title2.weight(.semibold))
            }
            .padding(16)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        }
        .foregroundStyle(.white)
        .aspectRatio(1.586, contentMode: .fit)
        .frame(maxWidth: 400)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .accessibilityElement(children: .combine)
    }
}

private struct InsuranceRow: View {
    let insurance: ProfileModel.Insurance

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: insurance.company == "FEMECV" ? "mountain.2" : "cross.case")
                .foregroundStyle(.tint).frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(insurance.company)
                    if insurance.isFemecvSync {
                        Text("FEMECV Sync").font(.caption2.weight(.semibold))
                            .padding(.horizontal, 6).padding(.vertical, 2)
                            .background(.tint.opacity(0.2), in: Capsule())
                    }
                }
                Text(insurance.policyNumber).font(.subheadline).foregroundStyle(.secondary)
                status.font(.footnote).foregroundStyle(insurance.status == .expired ? .red : .secondary)
            }
        }
    }

    @ViewBuilder private var status: some View {
        let date = insurance.date.formatted(date: .long, time: .omitted)
        switch insurance.status {
        case .active: LText("insurance_status_active", date)
        case .upcoming: LText("insurance_status_upcoming", date)
        case .expired: LText("insurance_status_expired", date)
        }
    }
}

// MARK: - Sheets

private struct AddInsuranceView: View {
    let model: ProfileModel
    @Environment(\.dismiss) private var dismiss

    @State private var company = ""
    @State private var policyNumber = ""
    @State private var validFrom = Date()
    @State private var validTo = Calendar.current.date(byAdding: .year, value: 1, to: Date()) ?? Date()
    @State private var documents: [URL] = []
    @State private var pickingDocuments = false
    @State private var isLoading = false
    @State private var failed = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("insurance_company", text: $company)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach(["FEMECV", "Rocalsub"], id: \.self) { suggestion in
                                Button(suggestion) { company = suggestion }.buttonStyle(.bordered)
                            }
                        }
                    }
                    TextField("insurance_policy_number", text: $policyNumber)
                    DatePicker("insurance_start_date", selection: $validFrom, displayedComponents: .date)
                    DatePicker("insurance_end_date", selection: $validTo, in: validFrom..., displayedComponents: .date)
                }
                Section("insurance_documents") {
                    ForEach(documents, id: \.self) { Text($0.lastPathComponent) }
                    Button("insurance_documents") { pickingDocuments = true }
                }
                Section { Text("insurance_add_info").font(.footnote).foregroundStyle(.secondary) }
                if failed {
                    Section { Text("error_unknown_generic").foregroundStyle(.red) }
                }
            }
            .disabled(isLoading)
            .navigationTitle("insurance_add_title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("submit", action: submit).disabled(isLoading || company.isEmpty || policyNumber.isEmpty)
                }
            }
            .fileImporter(isPresented: $pickingDocuments, allowedContentTypes: [.pdf], allowsMultipleSelection: true) { result in
                guard case .success(let urls) = result else { return }
                documents += urls.compactMap(Self.copyToTemporary)
            }
        }
    }

    private func submit() {
        isLoading = true
        failed = false
        Task {
            let stored = await model.addInsurance(company: company, policyNumber: policyNumber, from: validFrom, to: validTo, documents: documents)
            isLoading = false
            if stored { dismiss() } else { failed = true }
        }
    }

    /// A picked file can only be read while its security scope is open, so it's copied to somewhere the shared code can.
    private static func copyToTemporary(_ url: URL) -> URL? {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        let destination = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
            .appendingPathComponent(url.lastPathComponent)
        do {
            try FileManager.default.createDirectory(at: destination.deletingLastPathComponent(), withIntermediateDirectories: true)
            try FileManager.default.copyItem(at: url, to: destination)
            return destination
        } catch {
            return nil
        }
    }
}

private struct FemecvLoginView: View {
    let model: ProfileModel
    @Environment(\.dismiss) private var dismiss

    @State private var username = ""
    @State private var password = ""
    @State private var error: String?
    @State private var isLoading = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Image("logo-femecv", bundle: nil).resizable().scaledToFit().frame(maxHeight: 80)
                        .accessibilityLabel("femecv_logo")
                    TextField("femecv_username", text: $username)
                        .textContentType(.username).textInputAutocapitalization(.never).autocorrectionDisabled()
                    SecureField("femecv_password", text: $password).textContentType(.password)
                }
                if let error { Section { Text(error).foregroundStyle(.red) } }
                Section { Text("femecv_notice").font(.footnote).foregroundStyle(.secondary) }
            }
            .disabled(isLoading)
            .navigationTitle("femecv_sync_enable")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() }.disabled(isLoading) }
                ToolbarItem(placement: .confirmationAction) {
                    Button("femecv_sync_login", action: connect)
                        .disabled(isLoading || username.trimmingCharacters(in: .whitespaces).isEmpty || password.isEmpty)
                }
            }
        }
    }

    private func connect() {
        isLoading = true
        error = nil
        Task {
            let failure = await model.connectFemecv(username: username, password: password)
            isLoading = false
            if let failure { error = failure } else { dismiss() }
        }
    }
}

private struct JoinDepartmentView: View {
    let model: ProfileModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(model.joinableDepartments, id: \.id) { department in
                Button {
                    model.join(department)
                    dismiss()
                } label: {
                    HStack(spacing: 12) {
                        KotlinImage(key: AnyHashable(department.id)) {
                            (try? await ImageLoading.shared.image(container: department))
                        }
                        .frame(width: 40, height: 40)
                        Text(department.displayName).foregroundStyle(.primary)
                    }
                }
            }
            .navigationTitle("departments_join_choose")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("close") { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
    }
}
