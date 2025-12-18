package org.centrexcursionistalcoi.app.ui.utils

import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedLending

fun List<Department>?.departmentsCountBadge(): Int? {
    return orEmpty().sumOf { department ->
        // Count the amount of unconfirmed requests
        department.members.orEmpty().count { !it.confirmed }
    }.takeIf { it > 0 }
}

fun List<ReferencedLending>?.lendingsCountBadge(): Int? {
    return orEmpty().count { it.status().isPending() }.takeIf { it > 0 }
}
