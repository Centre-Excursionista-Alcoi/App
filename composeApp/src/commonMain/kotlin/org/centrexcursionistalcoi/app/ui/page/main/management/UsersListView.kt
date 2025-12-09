package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.page.main.profile.DepartmentsListCard
import org.centrexcursionistalcoi.app.ui.page.main.profile.InsurancesListCard
import org.centrexcursionistalcoi.app.ui.reusable.TooltipIcon
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.form.ReadOnlyFormField
import org.centrexcursionistalcoi.app.ui.utils.orUnknown
import org.centrexcursionistalcoi.app.ui.utils.unknown
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UsersListView(
    windowSizeClass: WindowSizeClass,
    users: List<UserData>?,
    members: List<Member>?,
    departments: List<Department>?,
    onPromote: (UserData) -> Job,
    onKickFromDepartment: (UserData, Department) -> Job,
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

    val userMemberNumbers = users.orEmpty().map { it.memberNumber }
    val filteredMembers = members.orEmpty().filter { it.memberNumber !in userMemberNumbers }

    ListView(
        windowSizeClass = windowSizeClass,
        items = (users.orEmpty() + filteredMembers).ifEmpty { null },
        itemIdProvider = { user -> (user as? Member)?.id?.toLong() ?: user.id },
        itemDisplayName = { user ->
            when (user) {
                is Member -> user.fullName
                is UserData -> user.fullName

                else -> "" // won't happen
            }
        },
        itemTextStyle = { user ->
            if ((user as? UserData)?.isDisabled == true || (user as? Member)?.status != Member.Status.ACTIVE) {
                LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)
            } else {
                LocalTextStyle.current
            }
        },
        emptyItemsText = stringResource(Res.string.management_no_users),
        itemLeadingContent = { user ->
            if (user is UserData) {
                TooltipIcon(
                    imageVector = MaterialSymbols.AccountCircle,
                    tooltip = stringResource(Res.string.management_user_registered),
                    positioning = TooltipAnchorPosition.Right,
                )
            } else if (user is Member) {
                TooltipIcon(
                    imageVector = MaterialSymbols.AccountCircleOff,
                    tooltip = stringResource(Res.string.management_user_not_registered),
                    positioning = TooltipAnchorPosition.Right,
                )
            }
        },
        itemTrailingContent = { user ->
            if (user !is UserData) return@ListView

            if (user.isDisabled) {
                TooltipIconButton(
                    imageVector = MaterialSymbols.PersonOff,
                    tooltip = stringResource(Res.string.management_user_disabled),
                    enabled = false,
                    positioning = TooltipAnchorPosition.Left,
                    onClick = {},
                )
            }
            if (user.lendingUser != null) {
                TooltipIconButton(
                    imageVector = MaterialSymbols.Inventory,
                    tooltip = stringResource(Res.string.management_user_signed_up_for_lendings),
                    enabled = false,
                    positioning = TooltipAnchorPosition.Left,
                    onClick = {},
                )
            }
            val hasActiveInsurances = user.insurances.any { it.isActive() }
            if (hasActiveInsurances) {
                TooltipIconButton(
                    imageVector = MaterialSymbols.HealthAndSafety,
                    tooltip = stringResource(Res.string.management_user_has_insurance),
                    enabled = false,
                    positioning = TooltipAnchorPosition.Left,
                    onClick = {},
                )
            }
        },
        itemToolbarActions = { user ->
            if (user !is UserData) return@ListView

            if (user.isDisabled) return@ListView
            if (!user.isAdmin()) {
                TooltipIconButton(
                    imageVector = MaterialSymbols.AddModerator,
                    tooltip = stringResource(Res.string.management_promote_user),
                    positioning = TooltipAnchorPosition.Left,
                    onClick = { promotingUser = user },
                )
            }
        },
        filters = mapOf(
            "signed_up_for_lendings" to Filter(
                { stringResource(Res.string.management_user_signed_up_for_lendings) },
                { (it as? UserData)?.lendingUser != null }),
            "has_active_insurances" to Filter(
                { stringResource(Res.string.management_user_has_insurance) },
                { u -> (u as? UserData)?.insurances.orEmpty().any { it.isActive() } }),
        ),
        sortByOptions = SortBy.defaults<Entity<*>> { user ->
            when (user) {
                is UserData -> user.fullName
                is Member -> user.fullName
                else -> ""
            }
        } + listOf(
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
        val isDisabled = (user as? UserData)?.isDisabled ?: false
        if (isDisabled) {
            Text(
                text = stringResource(Res.string.personal_info_disabled),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        val memberStatus = (user as? Member)?.status
        when (memberStatus) {
            Member.Status.ACTIVE -> Res.string.personal_info_not_registered
            Member.Status.INACTIVE -> Res.string.personal_info_not_a_member
            Member.Status.PENDING -> Res.string.personal_info_member_pending
            else -> null
        }?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (memberStatus == Member.Status.ACTIVE) LocalContentColor.current else MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        val fullName = when (user) {
            is UserData -> user.fullName
            is Member -> user.fullName
            else -> unknown()
        }
        ReadOnlyFormField(
            value = fullName,
            label = stringResource(Res.string.personal_info_full_name),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )

        val email = when (user) {
            is UserData -> user.email
            is Member -> user.email.orUnknown()
            else -> unknown()
        }
        ReadOnlyFormField(
            value = email,
            label = stringResource(Res.string.personal_info_email),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )

        if (user is UserData) {
            user.lendingUser?.let { lendingUser ->
                ReadOnlyFormField(
                    value = lendingUser.phoneNumber,
                    label = stringResource(Res.string.lending_signup_phone),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }

            val insurances = user.insurances
            if (insurances.isNotEmpty()) {
                InsurancesListCard(insurances)
            }

            DepartmentsListCard(
                userSub = user.sub,
                departments = departments,
                onJoinDepartmentRequested = null,
                onLeaveDepartmentRequested = { department ->
                    onKickFromDepartment(user, department)
                },
            )
        }
    }
}
