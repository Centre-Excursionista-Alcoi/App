package org.centrexcursionistalcoi.app.ui.composition

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow

val LocalNavigationBarVisibility = compositionLocalOf<MutableStateFlow<Boolean>?> { null }
