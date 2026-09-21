package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.delete
import cea_app.composeapp.generated.resources.management_no_departments
import cea_app.composeapp.generated.resources.management_qualification_create
import cea_app.composeapp.generated.resources.management_qualification_edit
import cea_app.composeapp.generated.resources.management_qualification_expired_on
import cea_app.composeapp.generated.resources.management_qualification_grant
import cea_app.composeapp.generated.resources.management_qualification_holders
import cea_app.composeapp.generated.resources.management_qualification_hide_holders
import cea_app.composeapp.generated.resources.management_qualification_no_expiry
import cea_app.composeapp.generated.resources.management_qualification_no_holders
import cea_app.composeapp.generated.resources.management_qualification_none
import cea_app.composeapp.generated.resources.management_qualification_revoke
import cea_app.composeapp.generated.resources.management_qualification_valid_until
import cea_app.composeapp.generated.resources.management_qualifications
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Department.Companion.departmentsWithRole
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.DepartmentRosterMember
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.data.isActiveAt
import org.centrexcursionistalcoi.app.data.lastValidDate
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.GrantQualificationDialog
import org.centrexcursionistalcoi.app.ui.dialog.QualificationDialog
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Delete
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Edit
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.centrexcursionistalcoi.app.viewmodel.management.QualificationsManagementViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The departments the viewer can grant qualifications in (holds [DepartmentRole.EXAMINER] in, which
 * [DepartmentRole.QUALIFICATIONS_MANAGER] and [DepartmentRole.ADMIN] imply), or all of them for a global admin.
 */
fun List<Department>?.qualificationDepartments(profile: ProfileResponse): List<Department>? =
    if (profile.isAdmin) this else this?.departmentsWithRole(profile, DepartmentRole.EXAMINER)

@Composable
fun QualificationsListView(model: QualificationsManagementViewModel = koinViewModel()) {
    val profile by model.profile.collectAsState()
    val departments by model.departments.collectAsState()
    val qualifications by model.qualifications.collectAsState()
    val grants by model.grants.collectAsState()
    val roster by model.roster.collectAsState()

    LaunchedEffect(Unit) { model.refresh() }

    val profileValue = profile
    if (profileValue == null) {
        LoadingBox()
        return
    }

    QualificationsListView(
        profile = profileValue,
        departments = departments,
        qualifications = qualifications,
        grants = grants,
        roster = roster,
        onCreate = model::create,
        onUpdate = model::update,
        onDelete = model::delete,
        onLoadGrants = model::loadGrants,
        onLoadRoster = model::loadRoster,
        onSearchRoster = model::searchRoster,
        onGrant = model::grant,
        onRevoke = model::revoke,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualificationsListView(
    profile: ProfileResponse,
    departments: List<Department>?,
    qualifications: List<Qualification>?,
    grants: Map<Uuid, List<QualificationGrant>>,
    roster: Map<Uuid, List<DepartmentRosterMember>>,
    onCreate: (departmentId: Uuid, name: String, description: String?) -> Job,
    onUpdate: (Qualification, name: String, description: String?) -> Job,
    onDelete: (Qualification) -> Job,
    onLoadGrants: (Qualification) -> Job,
    onLoadRoster: (departmentId: Uuid) -> Job,
    onSearchRoster: suspend (departmentId: Uuid, query: String) -> List<DepartmentRosterMember>,
    onGrant: (Qualification, userSub: String, expiresAt: kotlin.time.Instant?) -> Job,
    onRevoke: (Qualification, userSub: String) -> Job,
) {
    val visibleDepartments = remember(profile, departments) { departments.qualificationDepartments(profile) }
    // Defining/editing/deleting qualifications needs QUALIFICATIONS_MANAGER (which ADMIN implies), granting only EXAMINER
    val managerDepartmentIds = remember(profile, departments) {
        if (profile.isAdmin) departments.orEmpty().map { it.id }.toSet()
        else departments.orEmpty().departmentsWithRole(profile, DepartmentRole.QUALIFICATIONS_MANAGER).map { it.id }.toSet()
    }

    ListView(
        items = visibleDepartments,
        itemIdProvider = { it.id },
        itemDisplayName = { it.displayName },
        emptyItemsText = stringResource(Res.string.management_no_departments),
        // Departments are managed in their own tab: this one only lists them to reach their qualifications
        isCreatingSupported = false,
    ) { department ->
        DepartmentQualifications(
            department = department,
            qualifications = qualifications?.filter { it.departmentId == department.id }?.sortedBy { it.name.lowercase() },
            canManage = department.id in managerDepartmentIds,
            grants = grants,
            roster = roster[department.id].orEmpty(),
            onCreate = { name, description -> onCreate(department.id, name, description) },
            onUpdate = onUpdate,
            onDelete = onDelete,
            onLoadGrants = onLoadGrants,
            onLoadRoster = { onLoadRoster(department.id) },
            onSearchRoster = { query -> onSearchRoster(department.id, query) },
            onGrant = onGrant,
            onRevoke = onRevoke,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.DepartmentQualifications(
    department: Department,
    /** `null` while still loading. */
    qualifications: List<Qualification>?,
    canManage: Boolean,
    grants: Map<Uuid, List<QualificationGrant>>,
    roster: List<DepartmentRosterMember>,
    onCreate: (name: String, description: String?) -> Job,
    onUpdate: (Qualification, name: String, description: String?) -> Job,
    onDelete: (Qualification) -> Job,
    onLoadGrants: (Qualification) -> Job,
    onLoadRoster: () -> Job,
    onSearchRoster: suspend (query: String) -> List<DepartmentRosterMember>,
    onGrant: (Qualification, userSub: String, expiresAt: kotlin.time.Instant?) -> Job,
    onRevoke: (Qualification, userSub: String) -> Job,
) {
    var creating by remember(department.id) { mutableStateOf(false) }
    if (creating) {
        QualificationDialog(
            title = stringResource(Res.string.management_qualification_create),
            qualification = null,
            onSave = onCreate,
            onDismissRequested = { creating = false },
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.management_qualifications),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (canManage) {
            TooltipIconButton(
                imageVector = MaterialSymbols.Add,
                tooltip = stringResource(Res.string.management_qualification_create),
                positioning = TooltipAnchorPosition.Left,
                onClick = { creating = true },
            )
        }
    }

    when {
        qualifications == null -> InlineLoading()
        qualifications.isEmpty() -> Text(
            text = stringResource(Res.string.management_qualification_none),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            for (qualification in qualifications) {
                QualificationCard(
                    qualification = qualification,
                    canManage = canManage,
                    grants = grants[qualification.id],
                    roster = roster,
                    onUpdate = onUpdate,
                    onDelete = onDelete,
                    onLoadGrants = { onLoadGrants(qualification); onLoadRoster() },
                    onSearchRoster = onSearchRoster,
                    onGrant = onGrant,
                    onRevoke = onRevoke,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualificationCard(
    qualification: Qualification,
    canManage: Boolean,
    /** `null` until the holders have been loaded. */
    grants: List<QualificationGrant>?,
    roster: List<DepartmentRosterMember>,
    onUpdate: (Qualification, name: String, description: String?) -> Job,
    onDelete: (Qualification) -> Job,
    onLoadGrants: () -> Job,
    onSearchRoster: suspend (query: String) -> List<DepartmentRosterMember>,
    onGrant: (Qualification, userSub: String, expiresAt: kotlin.time.Instant?) -> Job,
    onRevoke: (Qualification, userSub: String) -> Job,
) {
    var editing by remember(qualification.id) { mutableStateOf(false) }
    var deleting by remember(qualification.id) { mutableStateOf(false) }
    var granting by remember(qualification.id) { mutableStateOf(false) }
    var showingHolders by remember(qualification.id) { mutableStateOf(false) }

    if (editing) {
        QualificationDialog(
            title = stringResource(Res.string.management_qualification_edit),
            qualification = qualification,
            onSave = { name, description -> onUpdate(qualification, name, description) },
            onDismissRequested = { editing = false },
        )
    }
    if (deleting) {
        DeleteDialog(
            item = qualification,
            displayName = { it.name },
            onDelete = { onDelete(qualification) },
            onDismissRequested = { deleting = false },
        )
    }
    if (granting) {
        GrantQualificationDialog(
            qualification = qualification,
            onSearch = onSearchRoster,
            onGrant = { userSub, expiresAt -> onGrant(qualification, userSub, expiresAt) },
            onDismissRequested = { granting = false },
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = qualification.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (canManage) {
                    TooltipIconButton(
                        imageVector = MaterialSymbols.Edit,
                        tooltip = stringResource(Res.string.management_qualification_edit),
                        positioning = TooltipAnchorPosition.Left,
                        onClick = { editing = true },
                    )
                    TooltipIconButton(
                        imageVector = MaterialSymbols.Delete,
                        tooltip = stringResource(Res.string.delete),
                        positioning = TooltipAnchorPosition.Left,
                        onClick = { deleting = true },
                    )
                }
            }
            qualification.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = { granting = true }) {
                    Text(stringResource(Res.string.management_qualification_grant))
                }
                TextButton(
                    onClick = {
                        showingHolders = !showingHolders
                        if (showingHolders) onLoadGrants()
                    }
                ) {
                    Text(
                        stringResource(
                            if (showingHolders) Res.string.management_qualification_hide_holders
                            else Res.string.management_qualification_holders
                        )
                    )
                }
            }

            if (showingHolders) {
                when {
                    grants == null -> InlineLoading()
                    grants.isEmpty() -> Text(
                        text = stringResource(Res.string.management_qualification_no_holders),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    else -> {
                        val now = remember(grants) { Clock.System.now() }
                        for (grant in grants.sortedBy { holderName(it, roster).lowercase() }) {
                            GrantRow(
                                name = holderName(grant, roster),
                                grant = grant,
                                isActive = grant.isActiveAt(now),
                                onRevoke = { onRevoke(qualification, grant.userSub) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A small spinner for content that's still loading inside a panel, unlike [LoadingBox], which fills all the space it gets. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InlineLoading() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        CircularWavyProgressIndicator()
    }
}

/** The holder's name if they're in the loaded [roster], otherwise their id: a former member isn't in it anymore. */
private fun holderName(grant: QualificationGrant, roster: List<DepartmentRosterMember>): String =
    roster.firstOrNull { it.sub == grant.userSub }?.fullName ?: grant.userSub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrantRow(
    name: String,
    grant: QualificationGrant,
    isActive: Boolean,
    onRevoke: () -> Job,
) {
    val lastValidDate: LocalDate? = grant.lastValidDate()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text("• $name", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = when {
                    lastValidDate == null -> stringResource(Res.string.management_qualification_no_expiry)
                    isActive -> stringResource(Res.string.management_qualification_valid_until, lastValidDate.toString())
                    else -> stringResource(Res.string.management_qualification_expired_on, lastValidDate.toString())
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        TooltipIconButton(
            imageVector = MaterialSymbols.Close,
            tooltip = stringResource(Res.string.management_qualification_revoke),
            positioning = TooltipAnchorPosition.Left,
            onClick = { onRevoke() },
        )
    }
}
