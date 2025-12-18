package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ConditionalBadge(
    badgeText: String?,
    content: @Composable () -> Unit,
) {
    if (badgeText != null) {
        BadgedBox(badge = { Badge { Text(badgeText) } }) {
            content()
        }
    } else {
        content()
    }
}
