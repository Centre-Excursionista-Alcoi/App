package org.centrexcursionistalcoi.app.ui.utils

import androidx.compose.ui.Modifier

/**
 * Applies the given [modifier] to this [Modifier] if [condition] is true.
 * Otherwise, returns this [Modifier] unchanged.
 */
fun Modifier.modIf(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        this.then(modifier(Modifier))
    } else {
        this
    }
}
