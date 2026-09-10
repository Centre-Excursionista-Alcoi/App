@file:OptIn(ExperimentalWasmJsInterop::class)

package app.centrexcursionistalcoi.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.kilua.Application
import dev.kilua.compose.root
import dev.kilua.core.IComponent
import dev.kilua.form.text.Text
import dev.kilua.form.text.passwordRef
import dev.kilua.form.text.textRef
import dev.kilua.html.BsBgColor
import dev.kilua.html.ButtonSize
import dev.kilua.html.ButtonStyle
import dev.kilua.html.a
import dev.kilua.html.bsButton
import dev.kilua.html.div
import dev.kilua.html.navLink
import dev.kilua.html.span
import dev.kilua.modal.confirm
import dev.kilua.modal.modalRef
import dev.kilua.rest.RemoteRequestException
import dev.kilua.rest.RestClient
import dev.kilua.rest.call
import dev.kilua.rest.postDynamic
import dev.kilua.routing.browserRouter
import dev.kilua.tabulator.Align
import dev.kilua.tabulator.ColumnDefinition
import dev.kilua.tabulator.Layout
import dev.kilua.tabulator.TabulatorOptions
import dev.kilua.tabulator.tabulator
import dev.kilua.theme.ThemeManager
import dev.kilua.toast.toast
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.centrexcursionistalcoi.app.data.AdminFileSummary
import org.centrexcursionistalcoi.app.data.AdminUserDetail
import org.centrexcursionistalcoi.app.data.AdminUserSummary
import org.centrexcursionistalcoi.app.request.ForcePasswordChangeRequest
import org.centrexcursionistalcoi.app.response.PagedResponse

private val restClient = RestClient()

private const val PAGE_SIZE = 50

class App : Application() {
    override fun start() {
        ThemeManager.init()
        root("root") {
            AdminApp()
        }
    }
}

@Composable
private fun IComponent.AdminApp() {
    browserRouter(contextPath = "/admin") {
        route("files") { view { AdminLayout { FilesPage() } } }
        route("users") {
            view { AdminLayout { UsersPage() } }
            string { view { sub -> AdminLayout { UserDetailPage(sub) } } }
        }
    }
}

private fun formatInstant(instant: Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.UTC)
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    return "${dt.date} $hour:$minute UTC"
}

@Composable
private fun IComponent.StatusBadge(disabled: Boolean) {
    if (disabled) {
        span(className = "badge bg-danger") { +"Disabled" }
    } else {
        span(className = "badge bg-success") { +"Active" }
    }
}

@Composable
private fun IComponent.AdminLayout(content: @Composable IComponent.() -> Unit) {
    div("container-fluid py-3") {
        div("d-flex align-items-center mb-3 gap-3") {
            div("h4 mb-0") { +"CEA Admin" }
            navLink(href = "/admin/files", label = "Files")
            navLink(href = "/admin/users", label = "Users")
        }
        content()
    }
}

@Composable
private fun IComponent.PaginationControls(page: PagedResponse<*>?, offset: Int, onOffsetChange: (Int) -> Unit) {
    if (page == null) return
    div("d-flex justify-content-between align-items-center mt-2") {
        div { +"Showing ${page.items.size} of ${page.total}" }
        div("btn-group") {
            bsButton(
                "Previous",
                style = ButtonStyle.BtnOutlineSecondary,
                size = ButtonSize.BtnSm,
                disabled = offset <= 0
            ) {
                onClick { onOffsetChange((offset - PAGE_SIZE).coerceAtLeast(0)) }
            }
            bsButton(
                "Next",
                style = ButtonStyle.BtnOutlineSecondary,
                size = ButtonSize.BtnSm,
                disabled = (offset + page.items.size).toLong() >= page.total
            ) {
                onClick { onOffsetChange(offset + PAGE_SIZE) }
            }
        }
    }
}

@Composable
private fun IComponent.FilesPage() {
    var offset by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf<PagedResponse<AdminFileSummary>?>(null) }
    val searchField = textRef(placeholder = "Search by file name...", className = "form-control mb-3")
    val query by searchField.stateFlow.collectAsState()

    LaunchedEffect(query, offset) {
        page = try {
            restClient.call<PagedResponse<AdminFileSummary>, Map<String, String>>(
                "/admin/api/files",
                buildMap {
                    query?.takeIf { it.isNotBlank() }?.let { put("q", it) }
                    put("limit", PAGE_SIZE.toString())
                    put("offset", offset.toString())
                }
            )
        } catch (e: RemoteRequestException) {
            toast("Failed to load files: ${e.message}", bgColor = BsBgColor.BgDanger)
            null
        }
    }

    // Keyed on `page`: Tabulator renders its initial `data` correctly on mount, but doesn't reliably pick up
    // reactive updates to `data` pushed from a coroutine (LaunchedEffect) afterwards -- forcing a full
    // remount whenever the fetched page changes sidesteps that instead of relying on its in-place update path.
    key(page) {
        tabulator<AdminFileSummary>(
            data = page?.items.orEmpty(),
            options = TabulatorOptions(
                layout = Layout.FitDataStretch,
                columns = listOf(
                    ColumnDefinition(title = "Name", field = "name"),
                    ColumnDefinition(title = "Type", field = "type"),
                    ColumnDefinition(title = "Size (bytes)", field = "sizeBytes", hozAlign = Align.Right),
                    ColumnDefinition(
                        title = "Last modified",
                        formatterComponentFunction = { _, _, data ->
                            div { +formatInstant(data.lastModified) }
                        }
                    ),
                    ColumnDefinition(
                        title = "",
                        formatterComponentFunction = { _, _, data ->
                            // Same-origin link, opened as a normal top-level navigation so the browser sends
                            // the admin session cookie automatically -- the server decides inline preview vs.
                            // download based on the file's content type.
                            a(href = "/admin/api/files/${data.id}/content", label = "View", target = "_blank")
                        }
                    ),
                )
            )
        )
    }

    PaginationControls(page, offset) { offset = it }
}

@Composable
private fun IComponent.UsersPage() {
    var offset by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf<PagedResponse<AdminUserSummary>?>(null) }
    val searchField = textRef(placeholder = "Search by name, email or NIF...", className = "form-control mb-3")
    val query by searchField.stateFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedUser by remember { mutableStateOf<AdminUserSummary?>(null) }
    var passwordField by remember { mutableStateOf<Text?>(null) }

    LaunchedEffect(query, offset) {
        page = try {
            restClient.call<PagedResponse<AdminUserSummary>, Map<String, String>>(
                "/admin/api/users",
                buildMap {
                    query?.takeIf { it.isNotBlank() }?.let { put("q", it) }
                    put("limit", PAGE_SIZE.toString())
                    put("offset", offset.toString())
                }
            )
        } catch (e: RemoteRequestException) {
            toast("Failed to load users: ${e.message}", bgColor = BsBgColor.BgDanger)
            null
        }
    }

    val passwordModal = modalRef(caption = "Force password change") {
        val self = this
        div("mb-2") { +"Set a new password for ${selectedUser?.fullName.orEmpty()}." }
        passwordField = passwordRef(placeholder = "New password (min. 8 chars, upper+lower+digit)")
        footer {
            bsButton("Set password", style = ButtonStyle.BtnWarning) {
                onClick {
                    val user = selectedUser ?: return@onClick
                    val newPassword = passwordField?.value.orEmpty()
                    scope.launch {
                        try {
                            restClient.postDynamic(
                                "/admin/api/users/${user.sub}/force-password",
                                ForcePasswordChangeRequest(newPassword)
                            )
                            toast("Password changed for ${user.fullName}")
                            self.hide()
                        } catch (_: RemoteRequestException) {
                            toast(
                                "Failed to change password. It may not be strong enough (min. 8 chars, upper+lower+digit).",
                                bgColor = BsBgColor.BgDanger
                            )
                        }
                    }
                }
            }
        }
    }

    // See the matching comment in FilesPage: forces a full Tabulator remount whenever the fetched page
    // changes, since in-place `data` updates pushed from a coroutine don't reliably reach Tabulator's own
    // reactive update path.
    key(page) {
        tabulator<AdminUserSummary>(
            data = page?.items.orEmpty(),
            options = TabulatorOptions(
                // FitColumns stretches every column to fill the container width, which looks absurdly wide
                // for narrow content like "#"/"Disabled" -- FitDataStretch sizes columns to their content
                // instead, only stretching the last one (Actions).
                layout = Layout.FitDataStretch,
                columns = listOf(
                    ColumnDefinition(title = "#", field = "memberNumber"),
                    ColumnDefinition(title = "Name", field = "fullName"),
                    ColumnDefinition(title = "Email", field = "email"),
                    ColumnDefinition(
                        title = "Status",
                        field = "isDisabled",
                        formatterComponentFunction = { _, _, data -> StatusBadge(data.isDisabled) }
                    ),
                    ColumnDefinition(
                        title = "Actions",
                        formatterComponentFunction = { _, _, data ->
                            div("d-flex gap-1") {
                                // Opens in a new tab -- a fresh page load, so it doesn't lose the current
                                // search/pagination state of the list underneath it.
                                a(
                                    href = "/admin/users/${data.sub}",
                                    label = "Details",
                                    target = "_blank",
                                    className = "btn btn-sm btn-outline-primary"
                                )
                                bsButton("Force password", style = ButtonStyle.BtnWarning, size = ButtonSize.BtnSm) {
                                    onClick {
                                        selectedUser = data
                                        passwordModal.show()
                                    }
                                }
                                bsButton(
                                    "Send recovery email",
                                    style = ButtonStyle.BtnSecondary,
                                    size = ButtonSize.BtnSm
                                ) {
                                    onClick {
                                        confirm(
                                            caption = "Send recovery email?",
                                            content = "This will email ${data.fullName} a password reset link.",
                                            cancelVisible = true,
                                            yesCallback = {
                                                scope.launch {
                                                    try {
                                                        restClient.postDynamic(
                                                            "/admin/api/users/${data.sub}/send-recovery-email",
                                                            Unit
                                                        )
                                                        toast("Recovery email sent to ${data.fullName}")
                                                    } catch (_: RemoteRequestException) {
                                                        toast("Failed to send recovery email", bgColor = BsBgColor.BgDanger)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                )
            )
        )
    }

    PaginationControls(page, offset) { offset = it }
}

@Composable
private fun IComponent.UserDetailPage(sub: String) {
    var detail by remember { mutableStateOf<AdminUserDetail?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(sub) {
        detail = try {
            restClient.call<AdminUserDetail>("/admin/api/users/$sub")
        } catch (e: RemoteRequestException) {
            loadFailed = true
            toast("Failed to load user: ${e.message}", bgColor = BsBgColor.BgDanger)
            null
        }
    }

    val current = detail
    if (current == null) {
        div { +(if (loadFailed) "Could not load this user." else "Loading...") }
        return
    }

    val profile = current.profile

    div("mb-4") {
        div("d-flex align-items-center gap-2 mb-2") {
            div("h4 mb-0") { +profile.fullName }
            StatusBadge(profile.isDisabled)
        }
        div { +"Email: ${profile.email}" }
        div { +"Member #${profile.memberNumber}" }
        div { +"Groups: ${profile.groups.joinToString(", ").ifEmpty { "(none)" }}" }

        if (profile.departments.isNotEmpty()) {
            div("mt-3") {
                div("fw-bold") { +"Departments" }
                profile.departments.forEach { dept ->
                    val roles = dept.roles.joinToString(", ").ifEmpty { "(none)" }
                    val confirmedLabel = if (dept.confirmed) "confirmed" else "pending"
                    div { +"${dept.departmentId} — roles: $roles ($confirmedLabel)" }
                }
            }
        }

        profile.lendingUser?.let { lendingUser ->
            div("mt-3") {
                div("fw-bold") { +"Lending program" }
                div { +"Phone: ${lendingUser.phoneNumber}" }
                div { +"Sports: ${lendingUser.sports.joinToString(", ").ifEmpty { "(none)" }}" }
            }
        }

        if (profile.insurances.isNotEmpty()) {
            div("mt-3") {
                div("fw-bold") { +"Insurances" }
                profile.insurances.forEach { insurance ->
                    div { +"${insurance.insuranceCompany} #${insurance.policyNumber} (${insurance.validFrom} to ${insurance.validTo})" }
                }
            }
        }
    }

    div("mb-4") {
        div("h5 mb-2") { +"Lendings (${current.lendings.size})" }
        if (current.lendings.isEmpty()) {
            div("text-muted") { +"No lendings." }
        }
        current.lendings.forEach { lending ->
            div("border rounded p-3 mb-2") {
                div("d-flex align-items-center gap-2 mb-2") {
                    div("fw-bold") { +"${lending.from} → ${lending.to}" }
                    if (lending.confirmed) {
                        span(className = "badge bg-success") { +"Confirmed" }
                    } else {
                        span(className = "badge bg-secondary") { +"Not confirmed" }
                    }
                    if (lending.taken) span(className = "badge bg-info text-dark") { +"Taken" }
                    if (lending.returned) span(className = "badge bg-success") { +"Returned" }
                    if (lending.memorySubmitted) span(className = "badge bg-info text-dark") { +"Memory submitted" }
                    if (lending.memoryReviewed) span(className = "badge bg-success") { +"Memory reviewed" }
                }
                div { +"Items: ${lending.items.joinToString(", ").ifEmpty { "(none)" }}" }
                div {
                    +if (lending.givenByName != null) {
                        "Given by ${lending.givenByName}" + (lending.givenAt?.let { " at ${formatInstant(it)}" } ?: "")
                    } else {
                        "Not picked up yet"
                    }
                }
                lending.notes?.let { notes -> div { +"Notes: $notes" } }
            }
        }
    }

    div {
        div("h5 mb-2") { +"Memories (${current.memories.size})" }
        if (current.memories.isEmpty()) {
            div("text-muted") { +"No memories." }
        }
        current.memories.forEach { memory ->
            div("border rounded p-3 mb-2") {
                div("fw-bold") { +"${memory.from} → ${memory.to}" }
                memory.place?.let { place -> div { +"Place: $place" } }
                memory.sport?.let { sport -> div { +"Sport: $sport" } }
                memory.externalUsers?.let { external -> div { +"External people: $external" } }
                div("mt-2") { +memory.text }
                div("mt-2 text-muted") { +"Submitted at ${formatInstant(memory.submittedAt)}" }
                if (memory.pdfId != null || memory.attachmentIds.isNotEmpty()) {
                    div("mt-2 d-flex gap-2") {
                        memory.pdfId?.let { pdfId ->
                            a(href = "/admin/api/files/$pdfId/content", label = "PDF", target = "_blank")
                        }
                        memory.attachmentIds.forEachIndexed { index, id ->
                            a(href = "/admin/api/files/$id/content", label = "Attachment ${index + 1}", target = "_blank")
                        }
                    }
                }
            }
        }
    }
}
