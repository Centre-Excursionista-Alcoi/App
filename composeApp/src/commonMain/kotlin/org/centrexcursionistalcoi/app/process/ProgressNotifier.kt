package org.centrexcursionistalcoi.app.process

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class ProgressNotifier(
    var context: String? = null,
    private val callback: suspend (Progress) -> Unit,
) {
    fun withContext(context: String) = ProgressNotifier(
        context = context,
        callback = callback,
    )

    suspend fun withContext(stringRes: StringResource) = withContext(getString(stringRes))

    suspend operator fun invoke(progress: Progress) = callback(progress)
}
