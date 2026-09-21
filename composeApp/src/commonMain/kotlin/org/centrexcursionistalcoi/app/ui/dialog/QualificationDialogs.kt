package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import cea_app.composeapp.generated.resources.form_description
import cea_app.composeapp.generated.resources.management_qualification_expires
import cea_app.composeapp.generated.resources.management_qualification_expires_on
import cea_app.composeapp.generated.resources.management_qualification_grant
import cea_app.composeapp.generated.resources.management_qualification_grant_no_members
import cea_app.composeapp.generated.resources.management_qualification_grant_search
import cea_app.composeapp.generated.resources.management_qualification_grant_title
import cea_app.composeapp.generated.resources.management_qualification_name
import cea_app.composeapp.generated.resources.save
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.DepartmentRosterMember
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.expiryInstant
import org.centrexcursionistalcoi.app.ui.reusable.form.DatePickerFormField
import org.centrexcursionistalcoi.app.ui.reusable.form.FormSwitchRow
import org.jetbrains.compose.resources.stringResource

/** Milliseconds to wait after the last keystroke before searching the department's members. */
private const val SEARCH_DEBOUNCE_MILLIS = 250L

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

/** Only days after today can be picked as the last day a grant is valid. */
@OptIn(ExperimentalMaterial3Api::class)
private val futureDatesOnly = object : SelectableDates {
    // The picker reports days as midnight UTC, so today's date is already in the past by now
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis > Clock.System.now().toEpochMilliseconds()
    override fun isSelectableYear(year: Int): Boolean = DatePickerDefaults.AllDates.isSelectableYear(year)
}

/**
 * Picks a member of the department to grant [qualification] to, optionally until a date. Members are searched by
 * name through [onSearch] (server-side, since a department can have hundreds of them).
 */
@Composable
fun GrantQualificationDialog(
    qualification: Qualification,
    onSearch: suspend (query: String) -> List<DepartmentRosterMember>,
    onGrant: (userSub: String, expiresAt: Instant?) -> Job,
    onDismissRequested: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DepartmentRosterMember>?>(null) }
    var selectedSub by remember { mutableStateOf<String?>(null) }
    var expires by remember { mutableStateOf(false) }
    var lastValidDate by remember { mutableStateOf<LocalDate?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Also runs for the empty query, listing the first members alphabetically
    LaunchedEffect(query) {
        delay(SEARCH_DEBOUNCE_MILLIS)
        results = onSearch(query)
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(stringResource(Res.string.management_qualification_grant_title, qualification.name)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(Res.string.management_qualification_grant_search)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                val members = results
                if (members != null && members.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.management_qualification_grant_no_members),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                Column(modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    for (member in members.orEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = member.sub == selectedSub,
                                    enabled = !isLoading,
                                    role = Role.RadioButton,
                                    onClick = { selectedSub = member.sub },
                                ),
                        ) {
                            RadioButton(selected = member.sub == selectedSub, onClick = null, enabled = !isLoading)
                            Text(member.fullName, modifier = Modifier.padding(start = 8.dp).padding(vertical = 8.dp))
                        }
                    }
                }

                FormSwitchRow(
                    checked = expires,
                    onCheckedChange = { expires = it },
                    label = stringResource(Res.string.management_qualification_expires),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !isLoading,
                )
                if (expires) {
                    DatePickerFormField(
                        value = lastValidDate,
                        onValueChange = { lastValidDate = it },
                        label = stringResource(Res.string.management_qualification_expires_on),
                        selectableDates = futureDatesOnly,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && selectedSub != null && (!expires || lastValidDate != null),
                onClick = {
                    isLoading = true
                    val expiresAt = lastValidDate?.takeIf { expires }?.expiryInstant()
                    onGrant(selectedSub!!, expiresAt).invokeOnCompletion {
                        isLoading = false
                        onDismissRequested()
                    }
                }
            ) { Text(stringResource(Res.string.management_qualification_grant)) }
        },
        dismissButton = {
            TextButton(enabled = !isLoading, onClick = onDismissRequested) { Text(stringResource(Res.string.close)) }
        },
    )
}
