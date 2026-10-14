package org.centrexcursionistalcoi.app

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.nav.LocalTransitionContext
import org.centrexcursionistalcoi.app.nav.Navigator
import org.centrexcursionistalcoi.app.ui.screen.ActivityMemoryEditor
import org.centrexcursionistalcoi.app.ui.screen.InventoryItemTypeDetailsScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingCreationScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingDetailsScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingSignUpScreen
import org.centrexcursionistalcoi.app.ui.screen.SecurityScreen
import org.centrexcursionistalcoi.app.ui.screen.SettingsScreen
import org.centrexcursionistalcoi.app.ui.screen.admin.LendingManagementScreen
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceDetailsScreen
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceLendingCreationScreen
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceLendingDetailsScreen

@PublishedApi
internal val entriesLog = logging()

/**
 * The screens that open over the main one, once the user is in. Shared by the app's own navigation ([MainApp]) and
 * by the Compose islands the native iOS UI embeds for the screens it doesn't replace yet.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
context(scope: SharedTransitionScope)
fun EntryProviderScope<NavKey>.contentEntries(navigator: Navigator) {
    destination<Destination.Security> {
        SecurityScreen(onBack = { navigator.goBack() })
    }
    destination<Destination.Settings> {
        SettingsScreen(
            onBack = {
                navigator.goBack()
            },
            onSecurityRequested = {
                navigator.navigate(Destination.Security)
            },
            onDeleteAccount = {
                navigator.navigateClearingStack(Destination.Loading)
            },
        )
    }

    destination<Destination.LendingDetails> { route ->
        val lendingId = route.lendingId

        LendingDetailsScreen(
            lendingId = lendingId,
            onMemoryEditorRequested = {
                navigator.navigate(Destination.LendingMemoryEditor(lendingId))
            },
            onBack = { navigator.goBack() }
        )
    }

    destination<Destination.ItemTypeDetails> { route ->
        val typeId = route.typeId
        val displayName = route.displayName

        InventoryItemTypeDetailsScreen(
            typeId = typeId,
            typeDisplayName = displayName,
            onBack = { navigator.goBack() },
        )
    }

    destination<Destination.Admin.LendingManagement> { route ->
        val lendingId = route.lendingId

        LendingManagementScreen(
            lendingId = lendingId,
            onBack = { navigator.goBack() },
        )
    }

    destination<Destination.LendingSignUp> {
        LendingSignUpScreen(
            onSignUpComplete = {
                navigator.navigatePoppingUpTo(Destination.Main(), Destination.Main::class)
            },
            onBackRequested = { navigator.goBack() }
        )
    }
    destination<Destination.LendingCreation> { route ->
        val items = route.shoppingList

        LaunchedEffect(items) {
            // If there are no items, go back
            if (items.isEmpty()) navigator.goBack()
        }

        LendingCreationScreen(
            originalShoppingList = items,
            onLendingCreated = {
                navigator.navigatePoppingUpTo(Destination.Main(), Destination.Main::class)
            }
        ) { navigator.goBack() }
    }
    destination<Destination.SpaceDetails> { route ->
        SpaceDetailsScreen(
            spaceId = route.spaceId,
            onBook = { navigator.navigate(Destination.SpaceLendingCreation(route.spaceId)) },
            onBack = { navigator.goBack() },
        )
    }
    destination<Destination.SpaceLendingCreation> { route ->
        SpaceLendingCreationScreen(
            spaceId = route.spaceId,
            lendingId = route.lendingId,
            onDone = { lendingId ->
                // Leave the booking form: show the lending in its place
                navigator.goBack()
                if (route.lendingId == null) navigator.navigate(Destination.SpaceLendingDetails(lendingId))
            },
            onBack = { navigator.goBack() },
        )
    }
    destination<Destination.SpaceLendingDetails> { route ->
        SpaceLendingDetailsScreen(
            lendingId = route.lendingId,
            onEdit = { lending -> navigator.navigate(Destination.SpaceLendingCreation(lending.space, lending.id)) },
            onBack = { navigator.goBack() },
        )
    }
    destination<Destination.LendingMemoryEditor> { route ->
        val lendingId = route.lendingId

        ActivityMemoryEditor(lendingId = lendingId, memoryId = null) { navigator.goBack() }
    }
    destination<Destination.MemoryEditor> { route ->
        val memoryId = route.memoryId

        ActivityMemoryEditor(lendingId = null, memoryId = memoryId) { navigator.goBack() }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
context(scope: SharedTransitionScope)
inline fun <reified D : Destination> EntryProviderScope<NavKey>.destination(
    noinline content: @Composable (D) -> Unit
) {
    entry<D> { route ->
        LaunchedEffect(Unit) {
            entriesLog.d { "Rendering screen ${D::class.simpleName}" }
        }

        val animatedContentScope = LocalNavAnimatedContentScope.current
        CompositionLocalProvider(LocalTransitionContext provides (scope to animatedContentScope)) {
            content(route)
        }
    }
}
