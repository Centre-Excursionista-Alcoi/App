package org.centrexcursionistalcoi.app.ui.page.home.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.ui.data.IconAction
import org.centrexcursionistalcoi.app.ui.dialog.InsuranceDialog
import org.centrexcursionistalcoi.app.ui.icons.BrandIcons
import org.centrexcursionistalcoi.app.ui.icons.FEMECV
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
                icon = Icons.Default.Add,
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
                            Icons.Default.HealthAndSafety,
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
