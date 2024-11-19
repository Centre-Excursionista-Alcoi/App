package org.centrexcursionistalcoi.app.data

import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.enumeration.ItemHealth
import org.jetbrains.compose.resources.StringResource

fun ItemHealth.localizedName(): StringResource = when (this) {
    ItemHealth.NEW -> Res.string.item_health_new
    ItemHealth.USED -> Res.string.item_health_used
    ItemHealth.DAMAGED -> Res.string.item_health_damaged
    ItemHealth.BROKEN -> Res.string.item_health_broken
}

fun ItemD.health(): StringResource = health.localizedName()
