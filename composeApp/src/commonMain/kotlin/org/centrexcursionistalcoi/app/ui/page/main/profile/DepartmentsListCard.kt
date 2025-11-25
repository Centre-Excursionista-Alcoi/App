package org.centrexcursionistalcoi.app.ui.page.main.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.data.IconAction
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.InformationCard
import org.jetbrains.compose.resources.stringResource

/**
 * A card displaying a list of departments the user is part of, with an option to join new ones.
 * @param profile The user's profile information.
 * @param departments The list of all departments, not just the joined ones.
 * @param onJoinDepartmentRequested A callback invoked when the user requests to join a department.
 */
@Composable
fun DepartmentsListCard(
    profile: ProfileResponse,
    departments: List<Department>?,
    onJoinDepartmentRequested: (Department) -> Job,
) {
    var requestedDepartmentJoin by remember { mutableStateOf(false) }
    if (requestedDepartmentJoin) {
        var isLoading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isLoading) requestedDepartmentJoin = false },
            title = { Text(stringResource(Res.string.departments_join_choose)) },
            text = {
                LazyColumn {
                    items(departments.orEmpty()) { department ->
                        ListItem(
                            leadingContent = {
                                val imageFile by department.rememberImageFile()
                                AsyncByteImage(imageFile)
                            },
                            headlineContent = {
                                Text(department.displayName)
                            },
                            modifier = Modifier.clickable {
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
                TextButton(onClick = { if (!isLoading) requestedDepartmentJoin = false }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    InformationCard(
        title = stringResource(Res.string.departments_title),
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        action = IconAction(
            icon = Icons.Default.Add,
            contentDescription = stringResource(Res.string.insurance_add_title),
            onClick = { requestedDepartmentJoin = true }
        )
    ) {
        val userDepartments = remember(profile, departments) {
            departments?.filter { dept -> dept.members.orEmpty().find { it.userSub == profile.sub } != null }.orEmpty()
        }
        for (department in userDepartments) {
            val memberInfo = department.members?.find { it.userSub == profile.sub } ?: continue
            ListItem(
                leadingContent = {
                    val imageFile by department.rememberImageFile()
                    AsyncByteImage(imageFile)
                },
                headlineContent = {
                    Text(department.displayName)
                },
                supportingContent = {
                    if (!memberInfo.confirmed) {
                        Text(stringResource(Res.string.departments_member_pending))
                    }
                }
            )
        }
        if (userDepartments.isEmpty()) {
            Text(stringResource(Res.string.departments_member_none))
        }
    }
}
