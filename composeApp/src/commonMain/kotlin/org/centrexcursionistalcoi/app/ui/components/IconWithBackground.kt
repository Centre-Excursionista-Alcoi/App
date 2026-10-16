package org.centrexcursionistalcoi.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Numbers
import org.centrexcursionistalcoi.app.ui.theme.AppTheme

@Composable
fun IconWithBackground(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(backgroundColor)
) {
    Surface(modifier = modifier, color = backgroundColor, contentColor = contentColor, shape = CircleShape) {
        Icon(icon, contentDescription, modifier = Modifier.padding(8.dp))
    }
}

@Preview
@Composable
fun IconWithBackground_Preview() {
    AppTheme {
        IconWithBackground(MaterialSymbols.Numbers, null)
    }
}
