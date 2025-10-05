package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveVerticalGrid(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyGridScope.() -> Unit
) {
    val columns = if (windowSizeClass.widthSizeClass > WindowWidthSizeClass.Medium) {
        // Desktop and large tablets
        GridCells.Adaptive(minSize = 300.dp)
    } else {
        // Phones and small tablets
        GridCells.Fixed(1)
    }
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        content()
    }
}
