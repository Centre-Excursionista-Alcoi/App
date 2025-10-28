package org.centrexcursionistalcoi.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal

val <T: Any> ProvidableCompositionLocal<T?>.currentOrThrow: T
    @Composable get() = current ?: error("No value provided for ${this::class.simpleName}")
