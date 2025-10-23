package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CardWithIcon(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = title,
    colors: CardColors = CardDefaults.outlinedCardColors(),
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
    }
}

@Preview
@Composable
fun CardWithIcon_Preview() {
    CardWithIcon("Example Card", "This is an example of a card with an icon.", Icons.Default.Info)
}
