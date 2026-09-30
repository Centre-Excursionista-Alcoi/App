package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.passkey_explanation_action
import cea_app.composeapp.generated.resources.passkey_explanation_message
import cea_app.composeapp.generated.resources.passkey_explanation_title
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Passkey
import org.jetbrains.compose.resources.stringResource

/**
 * Explains, in plain words, what a passkey is and why it's better than a password, with a button to create one.
 * Wherever the app offers passkeys: registering, the security settings, and the prompt to upgrade.
 */
@Composable
fun PasskeyExplanationCard(
    isLoading: Boolean,
    onCreatePasskey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(MaterialSymbols.Passkey, contentDescription = null, modifier = Modifier.size(32.dp))
                Text(
                    text = stringResource(Res.string.passkey_explanation_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = stringResource(Res.string.passkey_explanation_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                enabled = !isLoading,
                onClick = onCreatePasskey,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { Text(stringResource(Res.string.passkey_explanation_action)) }
        }
    }
}
