package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.page.main.profile.DepartmentsListCard
import org.centrexcursionistalcoi.app.ui.page.main.profile.InsurancesListCard
import org.centrexcursionistalcoi.app.ui.reusable.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.form.ReadOnlyFormField
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UsersListView(
    windowSizeClass: WindowSizeClass,
    users: List<UserData>?,
    departments: List<Department>?,
    onPromote: (UserData) -> Job,
) {
    var promotingUser by remember { mutableStateOf<UserData?>(null) }
    promotingUser?.let { user ->
        var isPromoting by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isPromoting) promotingUser = null },
            title = { Text(stringResource(Res.string.management_promote_user_title)) },
            text = { Text(stringResource(Res.string.management_promote_user_confirmation, user.fullName)) },
            confirmButton = {
                TextButton(
                    enabled = !isPromoting,
                    onClick = {
                        isPromoting = true
                        onPromote(user).invokeOnCompletion {
                            isPromoting = false
                            promotingUser = null
                        }
                    }
                ) { Text(stringResource(Res.string.management_promote_user)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !isPromoting,
                    onClick = { if (!isPromoting) promotingUser = null }
                ) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    ListView(
        windowSizeClass = windowSizeClass,
        items = users,
        itemIdProvider = { it.id },
        itemDisplayName = { it.fullName },
        itemTextStyle = { user ->
            if (user.isDisabled) {
                LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)
            } else {
                LocalTextStyle.current
            }
        },
        emptyItemsText = stringResource(Res.string.management_no_users),
        itemTrailingContent = { user ->
            if (user.isDisabled) {
                TooltipIconButton(
                    imageVector = Icons.Default.PersonOff,
                    tooltip = stringResource(Res.string.management_user_disabled),
                    enabled = false,
                    positioning = TooltipAnchorPosition.Left,
                    onClick = {},
                )
            }
            if (user.lendingUser != null) {
                TooltipIconButton(
                    imageVector = Icons.Default.Inventory2,
                    tooltip = stringResource(Res.string.management_user_signed_up_for_lendings),
                    enabled = false,
                    positioning = TooltipAnchorPosition.Left,
                    onClick = {},
                )
            }
            val hasActiveInsurances = user.insurances.any { it.isActive() }
            if (hasActiveInsurances) {
                TooltipIconButton(
                    imageVector = Icons.Default.HealthAndSafety,
                    tooltip = stringResource(Res.string.management_user_has_insurance),
                    enabled = false,
                    positioning = TooltipAnchorPosition.Left,
                    onClick = {},
                )
            }
        },
        itemToolbarActions = { user ->
            if (user.isDisabled) return@ListView
            if (!user.isAdmin()) {
                TooltipIconButton(
                    imageVector = Icons.Default.AddModerator,
                    tooltip = stringResource(Res.string.management_promote_user),
                    positioning = TooltipAnchorPosition.Left,
                    onClick = { promotingUser = user },
                )
            }
        },
        filters = mapOf(
            "signed_up_for_lendings" to Filter({ stringResource(Res.string.management_user_signed_up_for_lendings) }, { it.lendingUser != null }),
            "has_active_insurances" to Filter({ stringResource(Res.string.management_user_has_insurance) }, { u -> u.insurances.any { it.isActive() } }),
        ),
        sortByOptions = SortBy.defaults<UserData> { it.fullName } + listOf(
            /*SortBy(
                label = { stringResource(Res.string.sort_by_has_insurance_first) },
                sorted = { it.sortedBy { u -> u.insurances.isNotEmpty() } },
            ),
            SortBy(
                label = { stringResource(Res.string.sort_by_has_insurance_last) },
                sorted = { it.sortedBy { u -> u.insurances.isEmpty() } },
            ),
            SortBy(
                label = { stringResource(Res.string.sort_by_signed_lendings_first) },
                sorted = { it.sortedBy { u -> u.lendingUser != null } },
            ),
            SortBy(
                label = { stringResource(Res.string.sort_by_signed_lendings_last) },
                sorted = { it.sortedBy { u -> u.lendingUser == null } },
            ),*/
        ),
        // users cannot be created or edited
        isCreatingSupported = false,
        editItemContent = null,
    ) { user ->
        if (user.isDisabled) {
            Text(
                text = stringResource(Res.string.personal_info_disabled),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    Res.string.personal_info_disabled_reason,
                    stringResource(
                        when (user.disableReason) {
                            "status_baixa" -> Res.string.personal_info_disabled_reason_baixa
                            "status_pendent" -> Res.string.personal_info_disabled_reason_pendent
                            "status_unknown" -> Res.string.unknown
                            "not_in_cea_members" -> Res.string.personal_info_disabled_reason_removed
                            else -> Res.string.unknown
                        }
                    )
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }

        ReadOnlyFormField(
            value = user.fullName,
            label = stringResource(Res.string.personal_info_full_name),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )

        ReadOnlyFormField(
            value = user.email ?: stringResource(Res.string.none),
            label = stringResource(Res.string.personal_info_email),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            error = stringResource(Res.string.personal_info_email_none).takeIf { user.email == null },
        )

        user.lendingUser?.let { lendingUser ->
            ReadOnlyFormField(
                value = lendingUser.phoneNumber,
                label = stringResource(Res.string.lending_signup_phone),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }

        if (user.insurances.isNotEmpty()) {
            InsurancesListCard(user.insurances)
        }

        DepartmentsListCard(
            userSub = user.sub,
            departments = departments,
            onJoinDepartmentRequested = null,
        )
    }
}
