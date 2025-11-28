package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import org.centrexcursionistalcoi.app.utils.localizedInstantAsDateTime

@Composable
fun ReferencedPost.localizedDate(): String {
    return localizedInstantAsDateTime(date)
}
