package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveVerticalGrid(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyStaggeredGridScope.() -> Unit
) {
    val columns = if (windowSizeClass.widthSizeClass > WindowWidthSizeClass.Medium) {
        // Desktop and large tablets
        StaggeredGridCells.Adaptive(minSize = 300.dp)
    } else {
        // Phones and small tablets
        StaggeredGridCells.Fixed(1)
    }
    LazyVerticalStaggeredGrid(
        state = state,
        columns = columns,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        content()
    }
}
