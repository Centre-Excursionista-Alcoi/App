package org.centrexcursionistalcoi.app.ui.page.home.lending

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.lending_no_active_insurances_action
import cea_app.composeapp.generated.resources.lending_no_active_insurances_message
import cea_app.composeapp.generated.resources.lending_no_active_insurances_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoInsurancesCard(onAddInsuranceRequested: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.lending_no_active_insurances_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
        )
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
