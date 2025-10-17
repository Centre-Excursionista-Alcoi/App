package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun clickInteractionSource(onClick: suspend () -> Unit) = remember { MutableInteractionSource() }
    .also { interactionSource ->
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
                if (it is PressInteraction.Release) {
                    onClick()
                }
            }
        }
    }
