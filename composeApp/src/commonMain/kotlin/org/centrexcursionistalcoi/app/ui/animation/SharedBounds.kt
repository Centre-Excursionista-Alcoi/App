package org.centrexcursionistalcoi.app.ui.animation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.centrexcursionistalcoi.app.nav.LocalTransitionContext

/**
 * Applies shared bounds animation to the [Modifier] for the given [key].
 *
 * Uses the [LocalTransitionContext] to obtain the necessary scopes for the animation.
 * If the context is not available, returns the original [Modifier] without any modifications.
 */
@Composable
fun Modifier.sharedBounds(key: String): Modifier {
    val localTransitionContext = LocalTransitionContext.current ?: return this
    val (sharedTransitionScope, animatedContentScope) = localTransitionContext

    return with(sharedTransitionScope) {
        Modifier.sharedBounds(
            rememberSharedContentState(key),
            animatedContentScope
        )
    }
}
