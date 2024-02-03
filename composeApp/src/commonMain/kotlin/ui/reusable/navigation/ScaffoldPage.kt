package ui.reusable.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@ExperimentalFoundationApi
abstract class ScaffoldPage {
    abstract val icon: ImageVector
    open val contentDescription: String? = null

    @Composable
    abstract fun label(): String

    @Composable
    abstract fun PagerScope.PageContent()
}
