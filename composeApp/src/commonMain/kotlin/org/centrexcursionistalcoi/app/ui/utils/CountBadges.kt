package org.centrexcursionistalcoi.app.ui.utils

import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.ui.page.main.management.isActive
import org.centrexcursionistalcoi.app.ui.screen.spaces.stage

fun List<Department>?.departmentsCountBadge(): Int? {
    return orEmpty().sumOf { department ->
        // Count the amount of unconfirmed requests
        department.members.orEmpty().count { !it.confirmed }
    }.takeIf { it > 0 }
}

fun List<ReferencedLending>?.lendingsCountBadge(): Int? {
    return orEmpty().count { it.status().isPending() }.takeIf { it > 0 }
}

/** The space lendings that still need something from the managers: the keys handed over, taken back, or the payment. */
fun List<SpaceLending>?.spaceLendingsCountBadge(): Int? {
    return orEmpty().count { it.stage.isActive() }.takeIf { it > 0 }
}
