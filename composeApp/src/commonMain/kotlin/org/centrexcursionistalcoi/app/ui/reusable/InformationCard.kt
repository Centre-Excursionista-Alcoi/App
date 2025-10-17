package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.ui.data.IconAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationCard(
    title: String,
    modifier: Modifier = Modifier,
    action: IconAction? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            action?.let { (icon, description, tooltip, onClick) ->
                TooltipBox(
                    enableUserInput = tooltip != null,
                    state = rememberTooltipState(),
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Left),
                    tooltip = {
                        tooltip?.let { PlainTooltip { Text(it) } }
                    }
                ) {
                    IconButton(
                        onClick = onClick,
                    ) {
                        Icon(icon, contentDescription = description)
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            content()
        }
        Spacer(Modifier.height(16.dp))
    }
}
