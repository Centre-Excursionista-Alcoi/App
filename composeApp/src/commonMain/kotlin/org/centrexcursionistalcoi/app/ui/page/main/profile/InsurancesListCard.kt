package org.centrexcursionistalcoi.app.ui.page.main.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.insurance
import cea_app.composeapp.generated.resources.insurance_add_title
import cea_app.composeapp.generated.resources.insurance_status_active
import cea_app.composeapp.generated.resources.insurance_status_expired
import cea_app.composeapp.generated.resources.insurance_status_upcoming
import cea_app.composeapp.generated.resources.insurances_title
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.data.sortedForDisplay
import org.centrexcursionistalcoi.app.ui.data.IconAction
import org.centrexcursionistalcoi.app.ui.dialog.InsuranceDialog
import org.centrexcursionistalcoi.app.ui.icons.BrandIcons
import org.centrexcursionistalcoi.app.ui.icons.FEMECV
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.HealthAndSafety
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.InformationCard
import org.centrexcursionistalcoi.app.utils.localizedLocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
fun InsurancesListCard(
    insurances: List<UserInsurance>,
    onAddInsuranceRequested: (() -> Unit)? = null,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val sortedInsurances = remember(insurances, today) { insurances.sortedForDisplay(today) }

    var displayingInsurance by remember { mutableStateOf<UserInsurance?>(null) }
    displayingInsurance?.let {
        InsuranceDialog(
            insurance = it,
            onDismissRequest = { displayingInsurance = null }
        )
    }

    InformationCard(
        title = stringResource(Res.string.insurances_title),
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        action = if (onAddInsuranceRequested != null) {
            IconAction(
                icon = MaterialSymbols.Add,
                contentDescription = stringResource(Res.string.insurance_add_title),
                onClick = onAddInsuranceRequested
            )
        } else {
            null
        }
    ) {
        for (insurance in sortedInsurances) {
            ListItem(
                leadingContent = {
                    Icon(
                        if (insurance.insuranceCompany == "FEMECV")
                            BrandIcons.FEMECV
                        else
                            MaterialSymbols.HealthAndSafety,
                        stringResource(Res.string.insurance),
                        modifier = Modifier.size(32.dp)
                    )
                },
                headlineContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(insurance.insuranceCompany, style = MaterialTheme.typography.bodyLarge)
                        if (insurance.femecvLicense != null) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(start = 8.dp)
                            ) { Text("FEMECV Sync") }
                        }
                    }
                },
                supportingContent = {
                    Column {
                        Text(insurance.policyNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        InsuranceStatusText(insurance, today)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { displayingInsurance = insurance }
            )
        }
    }
}

@Composable
private fun InsuranceStatusText(insurance: UserInsurance, today: LocalDate) {
    val status = insurance.status(today)
    val text = when (status) {
        UserInsurance.Status.ACTIVE -> stringResource(Res.string.insurance_status_active, localizedLocalDate(insurance.validTo))
        UserInsurance.Status.UPCOMING -> stringResource(Res.string.insurance_status_upcoming, localizedLocalDate(insurance.validFrom))
        UserInsurance.Status.EXPIRED -> stringResource(Res.string.insurance_status_expired, localizedLocalDate(insurance.validTo))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (status == UserInsurance.Status.EXPIRED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
