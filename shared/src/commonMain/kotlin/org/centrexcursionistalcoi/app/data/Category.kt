package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class Category {
    MEMBER,
    NON_MEMBER,
    CHILD_MEMBER,
    CHILD_NON_MEMBER
}
