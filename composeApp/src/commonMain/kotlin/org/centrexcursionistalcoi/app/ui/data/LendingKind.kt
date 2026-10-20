package org.centrexcursionistalcoi.app.ui.data

/** What a lending is for: equipment from the inventory, or a space. */
enum class LendingKind { ITEMS, SPACE }

/** Which kinds of lendings the management page shows. */
enum class LendingKindFilter {
    ALL, ITEMS, SPACES;

    fun includes(kind: LendingKind): Boolean = when (this) {
        ALL -> true
        ITEMS -> kind == LendingKind.ITEMS
        SPACES -> kind == LendingKind.SPACE
    }
}
