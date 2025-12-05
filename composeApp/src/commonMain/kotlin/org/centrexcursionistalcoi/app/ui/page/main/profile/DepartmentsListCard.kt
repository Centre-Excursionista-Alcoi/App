package org.centrexcursionistalcoi.app.ui.page.main.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.ui.data.IconAction
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.InformationCard
import org.jetbrains.compose.resources.stringResource

/**
 * A card displaying a list of departments the user is part of, with an option to join new ones.
 * @param userSub The unique identifier of the user.
 * @param departments The list of all departments, not just the joined ones.
 * @param onJoinDepartmentRequested A callback invoked when the user requests to join a department.
 */
@Composable
fun DepartmentsListCard(
    userSub: String,
    departments: List<Department>?,
    onJoinDepartmentRequested: ((Department) -> Job)?,
    onLeaveDepartmentRequested: ((Department) -> Job)? = null,
) {
    val userDepartments = remember(userSub, departments) {
        departments?.filter { dept -> dept.members.orEmpty().find { it.userSub == userSub } != null }.orEmpty()
    }

    var requestedDepartmentJoin by remember { mutableStateOf(false) }
    if (requestedDepartmentJoin && onJoinDepartmentRequested != null) {
        var isLoading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isLoading) requestedDepartmentJoin = false },
            title = { Text(stringResource(Res.string.departments_join_choose)) },
            text = {
                LazyColumn {
                    items(
                        departments
                            // Filter departments the user is already a member of (in userDepartments)
                            ?.filter { dept -> userDepartments.find { it.id == dept.id } == null }
                            .orEmpty()
                    ) { department ->
                        val imageFile by department.rememberImageFile()
                        AsyncByteImage(
                            imageFile,
                            modifier = Modifier.aspectRatio(1f).clickable(enabled = !isLoading) {
                                isLoading = true
                                onJoinDepartmentRequested(department).invokeOnCompletion {
                                    isLoading = false
                                    requestedDepartmentJoin = false
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !isLoading, onClick = { requestedDepartmentJoin = false }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    var leavingDepartment by remember { mutableStateOf<Department?>(null) }
    leavingDepartment?.let { department ->
        var isLoading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isLoading) leavingDepartment = null },
            title = { Text(stringResource(Res.string.departments_leave_title)) },
            text = {
                Text(stringResource(Res.string.departments_leave_message, department.displayName))
            },
            dismissButton = {
                TextButton(enabled = !isLoading, onClick = { leavingDepartment = null }) {
                    Text(stringResource(Res.string.close))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        isLoading = true
                        onLeaveDepartmentRequested?.invoke(department)?.invokeOnCompletion {
                            isLoading = false
                            leavingDepartment = null
                        }
                    }
                ) {
                    Text(stringResource(Res.string.departments_leave))
                }
            }
        )
    }

    InformationCard(
        title = stringResource(Res.string.departments_title),
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        action = IconAction(
            icon = MaterialSymbols.Add,
            contentDescription = stringResource(Res.string.insurance_add_title),
            onClick = { requestedDepartmentJoin = true }
        ).takeIf { onJoinDepartmentRequested != null }
    ) {
        for (department in userDepartments) {
            val memberInfo = department.members?.find { it.userSub == userSub } ?: continue
            ListItem(
                leadingContent = {
                    val imageFile by department.rememberImageFile()
                    AsyncByteImage(imageFile, modifier = Modifier.size(36.dp))
                },
                headlineContent = {
                    Text(department.displayName)
                },
                supportingContent = {
                    if (!memberInfo.confirmed) {
                        Text(stringResource(Res.string.departments_member_pending))
                    }
                },
                modifier = Modifier.clickable { leavingDepartment = department },
            )
        }
        if (userDepartments.isEmpty()) {
            Text(stringResource(Res.string.departments_member_none))
        }
    }
}
