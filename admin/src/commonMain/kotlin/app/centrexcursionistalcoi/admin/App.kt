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
import dev.kilua.html.bsButton
import dev.kilua.html.div
import dev.kilua.html.navLink
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
import org.centrexcursionistalcoi.app.data.AdminFileSummary
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
        route("users") { view { AdminLayout { UsersPage() } } }
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
                layout = Layout.FitColumns,
                columns = listOf(
                    ColumnDefinition(title = "Name", field = "name"),
                    ColumnDefinition(title = "Type", field = "type"),
                    ColumnDefinition(title = "Size (bytes)", field = "sizeBytes", hozAlign = Align.Right),
                    ColumnDefinition(title = "Last modified", field = "lastModified"),
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
                layout = Layout.FitColumns,
                columns = listOf(
                    ColumnDefinition(title = "#", field = "memberNumber", width = "80px"),
                    ColumnDefinition(title = "Name", field = "fullName"),
                    ColumnDefinition(title = "Email", field = "email"),
                    ColumnDefinition(title = "Disabled", field = "isDisabled", width = "100px"),
                    ColumnDefinition(
                        title = "Actions",
                        formatterComponentFunction = { _, _, data ->
                            div("d-flex gap-1") {
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
