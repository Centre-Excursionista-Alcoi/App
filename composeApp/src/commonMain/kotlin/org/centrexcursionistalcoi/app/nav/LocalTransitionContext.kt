package org.centrexcursionistalcoi.app.nav

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

typealias TransitionContext = Pair<SharedTransitionScope, AnimatedContentScope>
val LocalTransitionContext = compositionLocalOf<TransitionContext?> { null }
