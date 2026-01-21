package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Delete
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.page.main.home.DepartmentPendingJoinRequest
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.form.FormImagePicker
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentsListView(
    windowSizeClass: WindowSizeClass,
    profile: ProfileResponse,
    users: List<UserData>?,
    departments: List<Department>?,
    onCreate: (displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onUpdate: (id: Uuid, displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onDelete: (Department) -> Job,
    onApproveDepartmentJoinRequest: (DepartmentMemberInfo) -> Job,
    onDenyDepartmentJoinRequest: (DepartmentMemberInfo) -> Job,
) {
    val filteredDepartments = remember(profile, departments) {
        if (profile.isAdmin) {
            // Admin can see all departments
            departments
        } else {
            // Non-admin can see only the departments they are managing
            departments?.filter { department ->
                department.members?.any { it.userSub == profile.sub && it.isManager } == true
            }
        }
    }

    var deleting by remember { mutableStateOf<Department?>(null) }
    deleting?.let { department ->
        DeleteDialog(
            item = department,
            displayName = { it.displayName },
            onDelete = { onDelete(department) },
            onDismissRequested = { deleting = null }
        )
    }

    ListView(
        windowSizeClass = windowSizeClass,
        items = filteredDepartments,
        itemIdProvider = { it.id },
        itemDisplayName = { it.displayName },
        itemLeadingContent = { department ->
            department.image ?: return@ListView
            val image by department.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = department.displayName,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
            )
        },
        itemTrailingContent = { department ->
            val unconfirmedRequests = department.members?.count { !it.confirmed } ?: 0
            if (unconfirmedRequests > 0) {
                Badge { Text(unconfirmedRequests.toString()) }
            }
        },
        emptyItemsText = stringResource(Res.string.management_no_departments),
        itemToolbarActions = {
            TooltipIconButton(
                imageVector = MaterialSymbols.Delete,
                tooltip = stringResource(Res.string.delete),
                onClick = { deleting = it }
            )
        },
        createTitle = stringResource(Res.string.management_department_create),
        editItemContent = { department: Department? ->
            var isLoading by remember { mutableStateOf(false) }
            var progress by remember { mutableStateOf<Progress?>(null) }
            var displayName by remember { mutableStateOf(department?.displayName ?: "") }
            var image by remember { mutableStateOf<PlatformFile?>(null) }

            val isDirty = if (department == null) true else displayName != department.displayName || image != null

            FormImagePicker(
                image = image,
                container = department,
                onImagePicked = { image = it },
                isLoading = isLoading,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)),
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.form_display_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(64.dp))

            LinearLoadingIndicator(progress)

            OutlinedButton(
                enabled = !isLoading && isDirty,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                onClick = {
                    isLoading = true
                    val job = if (department == null) {
                        onCreate(displayName, image) {
                            progress = it
                        }
                    } else {
                        onUpdate(department.id, displayName, image) {
                            progress = it
                        }
                    }
                    job.invokeOnCompletion {
                        isLoading = false
                        finishEdit()
                    }
                }
            ) {
                Text(stringResource(Res.string.submit))
            }
        },
    ) { department ->
        val members = remember(department) { department.members.orEmpty() }

        if (department.image != null) {
            val image by department.rememberImageFile()
            AsyncByteImage(
                bytes = image,
                contentDescription = department.displayName,
                modifier = Modifier.size(128.dp).clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(Modifier.height(12.dp))

        val pendingJoinRequests = remember(members) {
            members
                // Filter not confirmed requests
                .filterNot { it.confirmed }
        }
        if (pendingJoinRequests.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.management_other_users_join_requests),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
            )
            for (request in pendingJoinRequests) {
                val userData = remember(users) {
                    users?.find { it.sub == request.userSub }
                } ?: continue

                DepartmentPendingJoinRequest(
                    userData = userData,
                    department = department,
                    onApprove = { onApproveDepartmentJoinRequest(request) },
                    onDeny = { onDenyDepartmentJoinRequest(request) },
                )
            }
        }

        val confirmedMembers = remember(members, users) {
            members
                .filter { it.confirmed }
                .mapNotNull { memberInfo ->
                    users?.find { it.sub == memberInfo.userSub }
                }
                .sortedBy { it.fullName }
        }
        if (confirmedMembers.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.management_department_members),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            for (userData in confirmedMembers) {
                Text(
                    text = "\u2022 ${userData.fullName}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
