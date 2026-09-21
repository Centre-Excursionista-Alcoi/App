package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.event_qualifications_add_group
import cea_app.composeapp.generated.resources.event_qualifications_and
import cea_app.composeapp.generated.resources.event_qualifications_group_title
import cea_app.composeapp.generated.resources.event_qualifications_needs_department
import cea_app.composeapp.generated.resources.event_qualifications_none_in_department
import cea_app.composeapp.generated.resources.event_qualifications_remove_group
import cea_app.composeapp.generated.resources.event_qualifications_required
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.jetbrains.compose.resources.stringResource

/**
 * Edits an event's qualification requirements: a list of groups that must **all** be met (AND), each with the
 * [qualifications] that would meet it (OR: holding any one is enough). See `Event.qualificationRequirements`.
 *
 * A group with nothing selected is only a placeholder while editing; callers drop it when saving
 * (`normalizedRequirements()`).
 *
 * @param qualifications The department's qualifications, or `null` if the event has no department yet, since an
 *   event can only require its own department's.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QualificationRequirementsField(
    groups: List<List<Uuid>>,
    onGroupsChange: (List<List<Uuid>>) -> Unit,
    qualifications: List<Qualification>?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.event_qualifications_required),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        when {
            qualifications == null -> Text(
                text = stringResource(Res.string.event_qualifications_needs_department),
                style = MaterialTheme.typography.bodySmall,
            )
            qualifications.isEmpty() -> Text(
                text = stringResource(Res.string.event_qualifications_none_in_department),
                style = MaterialTheme.typography.bodySmall,
            )
            else -> {
                groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Text(
                            text = stringResource(Res.string.event_qualifications_and),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp, end = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(Res.string.event_qualifications_group_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TooltipIconButton(
                                    imageVector = MaterialSymbols.Close,
                                    tooltip = stringResource(Res.string.event_qualifications_remove_group),
                                    positioning = TooltipAnchorPosition.Left,
                                    enabled = enabled,
                                    onClick = { onGroupsChange(groups.filterIndexed { i, _ -> i != index }) },
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            ) {
                                for (qualification in qualifications) {
                                    val selected = qualification.id in group
                                    FilterChip(
                                        selected = selected,
                                        enabled = enabled,
                                        onClick = {
                                            val updated = if (selected) group - qualification.id else group + qualification.id
                                            onGroupsChange(groups.mapIndexed { i, g -> if (i == index) updated else g })
                                        },
                                        label = { Text(qualification.name) },
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    enabled = enabled,
                    onClick = { onGroupsChange(groups + listOf(emptyList())) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(Res.string.event_qualifications_add_group))
                }
            }
        }
    }
}
