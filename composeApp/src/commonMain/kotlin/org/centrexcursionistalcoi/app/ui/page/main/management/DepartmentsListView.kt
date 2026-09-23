package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.admin
import cea_app.composeapp.generated.resources.delete
import cea_app.composeapp.generated.resources.form_display_name
import cea_app.composeapp.generated.resources.management_department_create
import cea_app.composeapp.generated.resources.management_department_edit_qualifications
import cea_app.composeapp.generated.resources.management_department_edit_roles
import cea_app.composeapp.generated.resources.management_department_member_no_qualifications
import cea_app.composeapp.generated.resources.management_department_member_no_roles
import cea_app.composeapp.generated.resources.management_department_members
import cea_app.composeapp.generated.resources.management_no_departments
import cea_app.composeapp.generated.resources.management_other_users_join_requests
import cea_app.composeapp.generated.resources.management_qualification_create
import cea_app.composeapp.generated.resources.management_qualification_edit
import cea_app.composeapp.generated.resources.management_qualification_none
import cea_app.composeapp.generated.resources.management_qualifications
import cea_app.composeapp.generated.resources.submit
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Department.Companion.departmentsWithRole
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.data.isActiveAt
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.DepartmentMemberQualificationsDialog
import org.centrexcursionistalcoi.app.ui.dialog.DepartmentMemberRolesDialog
import org.centrexcursionistalcoi.app.ui.dialog.QualificationDialog
import org.centrexcursionistalcoi.app.ui.dialog.joinedDisplayNames
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Delete
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Edit
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Security
import org.centrexcursionistalcoi.app.ui.page.main.home.DepartmentPendingJoinRequest
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.form.FormImagePicker
import org.centrexcursionistalcoi.app.viewmodel.management.DepartmentsManagementViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Badge as BadgeIcon

@Composable
fun DepartmentsListView(model: DepartmentsManagementViewModel = koinViewModel()) {
    val profile = model.profile.collectAsState()
    val users by model.users.collectAsState()
    val departments by model.departments.collectAsState()

    val profileValue = profile.value
    if (profileValue == null) {
        LoadingBox()
        return
    }

    DepartmentsListView(
        profile = profileValue,
        users = users,
        departments = departments,
        onCreate = model::createDepartment,
        onUpdate = model::updateDepartment,
        onDelete = model::delete,
        onApproveDepartmentJoinRequest = model::approveDepartmentJoinRequest,
        onDenyDepartmentJoinRequest = model::denyDepartmentJoinRequest,
        onUpdateMemberRoles = model::updateMemberRoles,
        onCreateQualification = model::createQualification,
        onUpdateQualification = model::updateQualification,
        onDeleteQualification = model::deleteQualification,
        onUpdateMemberQualifications = model::updateMemberQualifications,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartmentsListView(
    profile: ProfileResponse,
    users: List<UserData>?,
    departments: List<Department>?,
    onCreate: (displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onUpdate: (id: Uuid, displayName: String, image: PlatformFile?, progressNotifier: ProgressNotifier?) -> Job,
    onDelete: (Department) -> Job,
    onApproveDepartmentJoinRequest: (DepartmentMemberInfo) -> Job,
    onDenyDepartmentJoinRequest: (DepartmentMemberInfo) -> Job,
    onUpdateMemberRoles: (DepartmentMemberInfo, List<DepartmentRole>) -> Job,
    onCreateQualification: (departmentId: Uuid, name: String, description: String?) -> Job,
    onUpdateQualification: (Qualification, name: String, description: String?) -> Job,
    onDeleteQualification: (Qualification) -> Job,
    onUpdateMemberQualifications: (DepartmentMemberInfo, currentlyHeldIds: Set<Uuid>, newlyHeldIds: Set<Uuid>) -> Job,
) {
    val filteredDepartments = remember(profile, departments) {
        if (profile.isAdmin) {
            // Admin can see all departments
            departments
        } else {
            // Non-admin can see only the departments they hold PEOPLE_MANAGER, EXAMINER (or QUALIFICATIONS_MANAGER/
            // ADMIN, which each imply one of those) in -- the latter two so an examiner who isn't also a people
            // manager can still reach the qualifications section below to grant/revoke.
            departments?.filter { department ->
                department.members?.any { member ->
                    member.userSub == profile.sub && member.confirmed &&
                        member.roles.any { it.implies(DepartmentRole.PEOPLE_MANAGER) || it.implies(DepartmentRole.EXAMINER) }
                } == true
            }
        }
    }
    // Editing/deleting a department's own fields (displayName, image) requires department ADMIN specifically --
    // PEOPLE_MANAGER (who can still see it above, to manage members/join requests) is not enough.
    val adminDepartmentIds = remember(profile, departments) {
        departments.orEmpty().departmentsWithRole(profile, DepartmentRole.ADMIN).map { it.id }.toSet()
    }
    // Creating/editing/deleting a qualification definition needs QUALIFICATIONS_MANAGER (ADMIN implies it).
    val qualificationsManagerDepartmentIds = remember(profile, departments) {
        departments.orEmpty().departmentsWithRole(profile, DepartmentRole.QUALIFICATIONS_MANAGER).map { it.id }.toSet()
    }
    // Granting/revoking an existing qualification only needs EXAMINER (QUALIFICATIONS_MANAGER/ADMIN imply it).
    val qualificationsExaminerDepartmentIds = remember(profile, departments) {
        departments.orEmpty().departmentsWithRole(profile, DepartmentRole.EXAMINER).map { it.id }.toSet()
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
        // Only a department's own ADMIN (or a global admin) may edit/delete it -- a PEOPLE_MANAGER can still see
        // it above (to manage members/join requests), but is not authorized to change its own fields.
        canModify = { department -> profile.isAdmin || department.id in adminDepartmentIds },
        // Creating a brand-new department always requires global admin: nobody can hold ADMIN in a department
        // that doesn't exist yet.
        isCreatingSupported = profile.isAdmin,
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
                        onCreate(displayName, image, ProgressNotifier {
                            progress = it
                        })
                    } else {
                        onUpdate(department.id, displayName, image,ProgressNotifier {
                            progress = it
                        })
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
        val canEditRoles = profile.isAdmin || department.id in adminDepartmentIds
        val canManageQualifications = profile.isAdmin || department.id in qualificationsManagerDepartmentIds
        val canGrantQualifications = profile.isAdmin || department.id in qualificationsExaminerDepartmentIds
        val qualifications = remember(department) { department.qualifications.orEmpty().sortedBy { it.name.lowercase() } }

        var editingRolesFor by remember(department) { mutableStateOf<Pair<DepartmentMemberInfo, UserData>?>(null) }
        editingRolesFor?.let { (memberInfo, userData) ->
            DepartmentMemberRolesDialog(
                memberName = userData.fullName,
                currentRoles = memberInfo.roles,
                onSave = { roles -> onUpdateMemberRoles(memberInfo, roles) },
                onDismissRequested = { editingRolesFor = null },
            )
        }

        var editingQualificationsFor by remember(department) { mutableStateOf<Pair<DepartmentMemberInfo, UserData>?>(null) }
        editingQualificationsFor?.let { (memberInfo, userData) ->
            val now = remember { Clock.System.now() }
            val currentlyHeldIds = remember(department, memberInfo) {
                department.qualificationGrants.orEmpty()
                    .filter { it.userSub == memberInfo.userSub && it.isActiveAt(now) }
                    .map { it.qualificationId }
                    .toSet()
            }
            DepartmentMemberQualificationsDialog(
                memberName = userData.fullName,
                qualifications = qualifications,
                currentlyHeldIds = currentlyHeldIds,
                onSave = { newlyHeldIds -> onUpdateMemberQualifications(memberInfo, currentlyHeldIds, newlyHeldIds) },
                onDismissRequested = { editingQualificationsFor = null },
            )
        }

        var creatingQualification by remember(department) { mutableStateOf(false) }
        if (creatingQualification) {
            QualificationDialog(
                title = stringResource(Res.string.management_qualification_create),
                qualification = null,
                onSave = { name, description -> onCreateQualification(department.id, name, description) },
                onDismissRequested = { creatingQualification = false },
            )
        }
        var editingQualification by remember(department) { mutableStateOf<Qualification?>(null) }
        editingQualification?.let { qualification ->
            QualificationDialog(
                title = stringResource(Res.string.management_qualification_edit),
                qualification = qualification,
                onSave = { name, description -> onUpdateQualification(qualification, name, description) },
                onDismissRequested = { editingQualification = null },
            )
        }
        var deletingQualification by remember(department) { mutableStateOf<Qualification?>(null) }
        deletingQualification?.let { qualification ->
            DeleteDialog(
                item = qualification,
                displayName = { it.name },
                onDelete = { onDeleteQualification(qualification) },
                onDismissRequested = { deletingQualification = null },
            )
        }

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
                // Map to user data
                .mapNotNull { info -> users?.find { it.sub == info.userSub }?.let { info to it } }
        }
        if (pendingJoinRequests.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.management_other_users_join_requests),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
            )
            for ((request, userData) in pendingJoinRequests) {
                DepartmentPendingJoinRequest(
                    userData = userData,
                    department = department,
                    onApprove = { onApproveDepartmentJoinRequest(request) },
                    onDeny = { onDenyDepartmentJoinRequest(request) },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                text = stringResource(Res.string.management_qualifications),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (canManageQualifications) {
                TooltipIconButton(
                    imageVector = MaterialSymbols.Add,
                    tooltip = stringResource(Res.string.management_qualification_create),
                    positioning = TooltipAnchorPosition.Left,
                    onClick = { creatingQualification = true },
                )
            }
        }
        if (qualifications.isEmpty()) {
            Text(
                text = stringResource(Res.string.management_qualification_none),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                for (qualification in qualifications) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(qualification.name, style = MaterialTheme.typography.bodyMedium)
                                qualification.description?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (canManageQualifications) {
                                TooltipIconButton(
                                    imageVector = MaterialSymbols.Edit,
                                    tooltip = stringResource(Res.string.management_qualification_edit),
                                    positioning = TooltipAnchorPosition.Left,
                                    onClick = { editingQualification = qualification },
                                )
                                TooltipIconButton(
                                    imageVector = MaterialSymbols.Delete,
                                    tooltip = stringResource(Res.string.delete),
                                    positioning = TooltipAnchorPosition.Left,
                                    onClick = { deletingQualification = qualification },
                                )
                            }
                        }
                    }
                }
            }
        }

        val confirmedMembers = remember(members, users) {
            members
                .filter { it.confirmed }
                .mapNotNull { memberInfo ->
                    users?.find { it.sub == memberInfo.userSub }?.let { memberInfo to it }
                }
                .sortedBy { (_, userData) -> userData.fullName }
        }
        if (confirmedMembers.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.management_department_members),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            val now = remember { Clock.System.now() }
            for ((memberInfo, userData) in confirmedMembers) {
                val heldQualificationNames = remember(department, memberInfo) {
                    val heldIds = department.qualificationGrants.orEmpty()
                        .filter { it.userSub == memberInfo.userSub && it.isActiveAt(now) }
                        .map { it.qualificationId }
                        .toSet()
                    qualifications.filter { it.id in heldIds }.map { it.name }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\u2022 ${userData.fullName}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // A global admin already has every permission everywhere: their department roles (if
                        // any are even set) are irrelevant, so show their admin status instead.
                        Text(
                            text = when {
                                userData.isAdmin() -> stringResource(Res.string.admin)
                                memberInfo.roles.isEmpty() -> stringResource(Res.string.management_department_member_no_roles)
                                else -> memberInfo.roles.joinedDisplayNames()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        Text(
                            text = heldQualificationNames.ifEmpty { listOf(stringResource(Res.string.management_department_member_no_qualifications)) }.joinToString(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (canGrantQualifications) {
                        TooltipIconButton(
                            imageVector = MaterialSymbols.BadgeIcon,
                            tooltip = stringResource(Res.string.management_department_edit_qualifications),
                            positioning = TooltipAnchorPosition.Left,
                            onClick = { editingQualificationsFor = memberInfo to userData },
                        )
                    }
                    // A global admin's own department roles can't grant them anything they don't already have,
                    // so editing them here would be a no-op -- hide the button rather than offer a dead action.
                    if (canEditRoles && !userData.isAdmin()) {
                        TooltipIconButton(
                            imageVector = MaterialSymbols.Security,
                            tooltip = stringResource(Res.string.management_department_edit_roles),
                            positioning = TooltipAnchorPosition.Left,
                            onClick = { editingRolesFor = memberInfo to userData },
                        )
                    }
                }
            }
        }
    }
}
