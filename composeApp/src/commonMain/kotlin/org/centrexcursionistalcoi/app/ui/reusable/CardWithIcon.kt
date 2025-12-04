package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Info
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CardWithIcon(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = title,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    OutlinedCard(
        modifier = modifier,
        colors = colors,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.padding(8.dp)
            )
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp).padding(end = 8.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        actions?.let { block ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)) {
                block()
            }
        }
    }
}

@Preview
@Composable
private fun CardWithIcon_Preview() {
    CardWithIcon("Example Card", "This is an example of a card with an icon.", MaterialSymbols.Info)
}
