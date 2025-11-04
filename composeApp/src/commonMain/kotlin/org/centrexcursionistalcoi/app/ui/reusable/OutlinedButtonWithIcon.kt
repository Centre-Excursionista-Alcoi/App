package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OutlinedButtonWithIcon(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = text,
    onClick: () -> Unit,
) {
    OutlinedButton(
        enabled = enabled,
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(icon, contentDescription)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
    }
}
