package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
 * @param content A block which describes the content. Inside this block you can use methods like [LazyListScope.item] to add a single item
 * or [LazyListScope.items] to add a list of items.
 */
@Composable
fun LazyColumnWidthWrapper(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 600.dp,
    content: LazyListScope.() -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(
            modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
        ) {
            content()
        }
    }
}
