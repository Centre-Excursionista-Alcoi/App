package org.centrexcursionistalcoi.app.ui.page.home.management

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.jetbrains.compose.resources.stringResource

@Composable
fun PostsListView(windowSizeClass: WindowSizeClass, posts: List<ReferencedPost>?) {
    ListView(
        windowSizeClass = windowSizeClass,
        items = posts,
        itemIdProvider = { it.id },
        itemDisplayName = { it.title },
        emptyItemsText = stringResource(Res.string.management_no_posts),
        // TODO: Enable post creation when implemented
        isCreatingSupported = false,
        editItemContent = null,
    ) { post ->
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(Res.string.post_by, post.department?.displayName ?: "Centre Excursionista d'Alcoi"),
        )

        Text(
            text = post.content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
