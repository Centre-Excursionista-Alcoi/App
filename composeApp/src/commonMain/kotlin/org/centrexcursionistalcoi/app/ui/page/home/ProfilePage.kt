package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.AddInsuranceDialog
import org.centrexcursionistalcoi.app.ui.dialog.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.ui.dialog.InsuranceDialog
import org.centrexcursionistalcoi.app.ui.page.home.profile.InsurancesListCard
import org.centrexcursionistalcoi.app.ui.page.home.profile.NoInsurancesCard
import org.centrexcursionistalcoi.app.ui.page.home.profile.PersonalInformationCard
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid

@Composable
fun ProfilePage(
    windowSizeClass: WindowSizeClass,
    profile: ProfileResponse,
    onCreateInsurance: CreateInsuranceRequest,
) {
    val activeInsurances = remember(profile) { profile.activeInsurances() }

    var addingInsurance by remember { mutableStateOf(false) }
    if (addingInsurance) AddInsuranceDialog(
        onCreate = onCreateInsurance,
        onDismissRequest = { addingInsurance = false }
    )

    var displayingInsurance by remember { mutableStateOf<UserInsurance?>(null) }
    displayingInsurance?.let {
        InsuranceDialog(
            insurance = it,
            onDismissRequest = { displayingInsurance = null }
        )
    }

    AdaptiveVerticalGrid(
        windowSizeClass,
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item("personal_info") {
            PersonalInformationCard(profile)
        }
        if (activeInsurances.isEmpty()) item("no_insurances") {
            NoInsurancesCard(
                onAddInsuranceRequested = { addingInsurance = true }
            )
        } else item("insurances_list") {
            InsurancesListCard(
                activeInsurances = activeInsurances,
                onAddInsuranceRequested = { addingInsurance = true },
                onInsuranceRequested = { displayingInsurance = it }
            )
        }
    }
}
