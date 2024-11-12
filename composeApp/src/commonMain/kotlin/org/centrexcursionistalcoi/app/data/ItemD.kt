package org.centrexcursionistalcoi.app.data

import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.enumeration.ItemHealth
import org.jetbrains.compose.resources.StringResource

fun ItemD.health(): StringResource = when (health) {
    ItemHealth.NEW -> Res.string.item_health_new
    ItemHealth.USED -> Res.string.item_health_used
    ItemHealth.DAMAGED -> Res.string.item_health_damaged
    ItemHealth.BROKEN -> Res.string.item_health_broken
}
