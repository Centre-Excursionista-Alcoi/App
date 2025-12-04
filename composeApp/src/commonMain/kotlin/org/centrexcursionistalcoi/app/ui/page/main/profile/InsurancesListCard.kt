package org.centrexcursionistalcoi.app.ui.page.main.profile

import androidx.compose.foundation.clickable
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
import cea_app.composeapp.generated.resources.active_insurances_title
import cea_app.composeapp.generated.resources.insurance
import cea_app.composeapp.generated.resources.insurance_add_title
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.ui.data.IconAction
import org.centrexcursionistalcoi.app.ui.dialog.InsuranceDialog
import org.centrexcursionistalcoi.app.ui.icons.BrandIcons
import org.centrexcursionistalcoi.app.ui.icons.FEMECV
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.HealthAndSafety
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.InformationCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun InsurancesListCard(
    activeInsurances: List<UserInsurance>,
    onAddInsuranceRequested: (() -> Unit)? = null,
) {
    var displayingInsurance by remember { mutableStateOf<UserInsurance?>(null) }
    displayingInsurance?.let {
        InsuranceDialog(
            insurance = it,
            onDismissRequest = { displayingInsurance = null }
        )
    }

    InformationCard(
        title = stringResource(Res.string.active_insurances_title),
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
        for (insurance in activeInsurances) {
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
                supportingContent = { Text(insurance.policyNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().clickable { displayingInsurance = insurance }
            )
        }
    }
}
