package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveVerticalGrid(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    gridMinSize: Dp = 300.dp,
    content: LazyGridScope.() -> Unit
) {
    val columns = if (windowSizeClass.widthSizeClass > WindowWidthSizeClass.Medium) {
        // Desktop and large tablets
        GridCells.Adaptive(minSize = gridMinSize)
    } else {
        // Phones and small tablets
        GridCells.Fixed(1)
    }
    LazyVerticalGrid(
        state = state,
        columns = columns,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        content()
    }
}
