package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import cea_app.composeapp.generated.resources.form_description
import cea_app.composeapp.generated.resources.management_department_member_no_qualifications
import cea_app.composeapp.generated.resources.management_department_member_qualifications_title
import cea_app.composeapp.generated.resources.management_qualification_name
import cea_app.composeapp.generated.resources.save
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.ui.reusable.form.FormSwitchRow
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

/**
 * Creates a qualification ([qualification] is `null`) or edits one: its name and description.
 * [onSave] gets the trimmed name and the trimmed description, `null` if it's blank.
 */
@Composable
fun QualificationDialog(
    title: String,
    qualification: Qualification?,
    onSave: (name: String, description: String?) -> Job,
    onDismissRequested: () -> Unit,
) {
    var name by remember(qualification) { mutableStateOf(qualification?.name ?: "") }
    var description by remember(qualification) { mutableStateOf(qualification?.description ?: "") }
    var isLoading by remember { mutableStateOf(false) }

    val trimmedName = name.trim()
    val trimmedDescription = description.trim().takeIf { it.isNotEmpty() }
    val isDirty = qualification == null || trimmedName != qualification.name || trimmedDescription != qualification.description

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.management_qualification_name)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.form_description)) },
                    minLines = 2,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && trimmedName.isNotEmpty() && isDirty,
                onClick = {
                    isLoading = true
                    onSave(trimmedName, trimmedDescription).invokeOnCompletion {
                        isLoading = false
                        onDismissRequested()
                    }
                }
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(enabled = !isLoading, onClick = onDismissRequested) { Text(stringResource(Res.string.close)) }
        },
    )
}

/**
 * Lets an examiner (or qualifications manager/department admin/global admin) replace [memberName]'s full set of
 * held [qualifications] in this department, the same toggle-switch shape [org.centrexcursionistalcoi.app.ui.dialog.DepartmentMemberRolesDialog]
 * already uses for roles. Unlike the old per-qualification "grant" flow, a grant made here never expires -- pick
 * [org.centrexcursionistalcoi.app.network.QualificationsRemoteRepository.grant] directly (outside this dialog) if
 * an expiring grant is ever needed again.
 */
@Composable
fun DepartmentMemberQualificationsDialog(
    memberName: String,
    qualifications: List<Qualification>,
    currentlyHeldIds: Set<Uuid>,
    onSave: (heldIds: Set<Uuid>) -> Job,
    onDismissRequested: () -> Unit,
) {
    var selectedIds by remember(currentlyHeldIds) { mutableStateOf(currentlyHeldIds) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(stringResource(Res.string.management_department_member_qualifications_title, memberName)) },
        text = {
            if (qualifications.isEmpty()) {
                Text(stringResource(Res.string.management_department_member_no_qualifications))
            } else {
                Column {
                    for (qualification in qualifications) {
                        FormSwitchRow(
                            checked = qualification.id in selectedIds,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + qualification.id else selectedIds - qualification.id
                            },
                            label = qualification.name,
                            description = qualification.description,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            enabled = !isLoading,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && selectedIds != currentlyHeldIds,
                onClick = {
                    isLoading = true
                    onSave(selectedIds).invokeOnCompletion {
                        isLoading = false
                        onDismissRequested()
                    }
                }
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(enabled = !isLoading, onClick = onDismissRequested) { Text(stringResource(Res.string.close)) }
        },
    )
}
