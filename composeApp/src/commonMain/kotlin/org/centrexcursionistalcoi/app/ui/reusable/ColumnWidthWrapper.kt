package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A wrapper that centers its content and restricts its maximum width.
 *
 * @param modifier Modifier to be applied to the outer Column.
 * @param maxWidth Maximum width for the inner Column. Default is `600dp`.
 * @param content Composable content to be displayed inside the inner Column.
 */
@Composable
fun ColumnWidthWrapper(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 600.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            horizontalAlignment = horizontalAlignment,
        ) {
            content()
        }
    }
}
