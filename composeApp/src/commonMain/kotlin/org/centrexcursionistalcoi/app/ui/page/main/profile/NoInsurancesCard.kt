package org.centrexcursionistalcoi.app.ui.page.main.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.ui.reusable.InformationCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoInsurancesCard(onAddInsuranceRequested: () -> Unit) {
    InformationCard(
        title = stringResource(Res.string.lending_no_active_insurances_title),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.lending_no_active_insurances_message),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
        )
        TextButton(
            onClick = onAddInsuranceRequested,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) { Text(stringResource(Res.string.lending_no_active_insurances_action)) }
    }
}

@Preview
@Composable
fun NoInsurancesCard_Preview() {
    NoInsurancesCard(onAddInsuranceRequested = {})
}
