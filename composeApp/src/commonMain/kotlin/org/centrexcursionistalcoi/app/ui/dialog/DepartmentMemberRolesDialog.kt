package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import cea_app.composeapp.generated.resources.department_role_admin
import cea_app.composeapp.generated.resources.department_role_content_manager
import cea_app.composeapp.generated.resources.department_role_inventory_manager
import cea_app.composeapp.generated.resources.department_role_lending_manager
import cea_app.composeapp.generated.resources.department_role_memory_manager
import cea_app.composeapp.generated.resources.department_role_people_manager
import cea_app.composeapp.generated.resources.management_department_member_roles_title
import cea_app.composeapp.generated.resources.save
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.ui.reusable.form.FormSwitchRow
import org.jetbrains.compose.resources.stringResource

@Composable
fun DepartmentRole.displayName(): String = when (this) {
    DepartmentRole.ADMIN -> stringResource(Res.string.department_role_admin)
    DepartmentRole.PEOPLE_MANAGER -> stringResource(Res.string.department_role_people_manager)
    DepartmentRole.INVENTORY_MANAGER -> stringResource(Res.string.department_role_inventory_manager)
    DepartmentRole.LENDING_MANAGER -> stringResource(Res.string.department_role_lending_manager)
    DepartmentRole.MEMORY_MANAGER -> stringResource(Res.string.department_role_memory_manager)
    DepartmentRole.CONTENT_MANAGER -> stringResource(Res.string.department_role_content_manager)
}

/**
 * Comma-joined [displayName]s. A plain `for` loop, not `map`/`joinToString`'s lambda -- neither is declared
 * `@Composable`, so calling a `@Composable` function (like [displayName]) inside either would not compile.
 */
@Composable
fun List<DepartmentRole>.joinedDisplayNames(): String {
    val labels = mutableListOf<String>()
    for (role in this) {
        labels.add(role.displayName())
    }
    return labels.joinToString()
}

/**
 * Lets a department ADMIN (or global admin) replace [memberName]'s full set of [DepartmentRole]s in this
 * department (`PATCH /departments/{id}/members/{memberId}/roles`). [DepartmentRole.ADMIN] implies every other
 * role server-side, but is still listed and toggled like any other role here rather than being a hidden
 * implicit state.
 */
@Composable
fun DepartmentMemberRolesDialog(
    memberName: String,
    currentRoles: List<DepartmentRole>,
    onSave: (List<DepartmentRole>) -> Job,
    onDismissRequested: () -> Unit,
) {
    var selectedRoles by remember(currentRoles) { mutableStateOf(currentRoles.toSet()) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(stringResource(Res.string.management_department_member_roles_title, memberName)) },
        text = {
            Column {
                for (role in DepartmentRole.entries) {
                    FormSwitchRow(
                        checked = role in selectedRoles,
                        onCheckedChange = { checked ->
                            selectedRoles = if (checked) selectedRoles + role else selectedRoles - role
                        },
                        label = role.displayName(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && selectedRoles != currentRoles.toSet(),
                onClick = {
                    isLoading = true
                    onSave(selectedRoles.toList()).invokeOnCompletion {
                        isLoading = false
                        onDismissRequested()
                    }
                }
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = onDismissRequested
            ) { Text(stringResource(Res.string.close)) }
        },
    )
}
