package org.centrexcursionistalcoi.app.nav

import org.centrexcursionistalcoi.app.FeatureFlag
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Department.Companion.isManagerOfAny
import org.centrexcursionistalcoi.app.hasFeature
import org.centrexcursionistalcoi.app.response.ProfileResponse

/** The pages of the main screen, in the order they are shown. */
enum class MainTab {
    HOME, LENDINGS, LENDING, ACTIVITIES, MANAGEMENT, PROFILE
}

/** Manages spaces or their lendings, and the spaces feature is enabled for them. */
fun ProfileResponse.isSpacesFeatureManager() =
    hasFeature(FeatureFlag.SPACES) && (isSpacesManager || isSpaceLendingsManager)

/** Whether [MainTab.MANAGEMENT] is for this user: admins, global managers and managers of a department. */
fun ProfileResponse.canSeeManagement(departments: List<Department>?): Boolean =
    isAdmin || isUsersManager || isMembersManager || isSpacesFeatureManager() || departments.orEmpty().isManagerOfAny(this)

/**
 * The tabs [profile] gets. Lending (their ongoing lending) replaces Lendings (browsing the gear) while they have one.
 *
 * Both the Compose and the native UIs build their tab bar from this, so they always agree.
 */
fun mainTabs(profile: ProfileResponse, departments: List<Department>?, hasActiveLending: Boolean): List<MainTab> = buildList {
    add(MainTab.HOME)
    add(if (hasActiveLending) MainTab.LENDING else MainTab.LENDINGS)
    add(MainTab.ACTIVITIES)
    if (profile.canSeeManagement(departments)) add(MainTab.MANAGEMENT)
    add(MainTab.PROFILE)
}
