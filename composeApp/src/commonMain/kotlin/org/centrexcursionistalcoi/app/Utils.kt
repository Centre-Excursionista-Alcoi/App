package org.centrexcursionistalcoi.app

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope

/**
 * A function that provides the maximum span for a grid item.
 * The default implementation returns a [GridItemSpan] with the maximum line span.
 *
 * Normally used in combination with [LazyGridItemSpanScope.item] to specify the span of a grid item. Example:
 * ```kotlin
 * LazyVerticalGrid(columns = GridCells.Adaptive(200.dp)) {
 *    item(key = item.id, span = maxGridItemSpan) {
 *        // Content
 *    }
 * }
 * ```
 * @see GridItemSpan
 */
val maxGridItemSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
