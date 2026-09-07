package org.centrexcursionistalcoi.app.nav

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Registers every concrete [Destination] as a [NavKey] subtype.
 *
 * Required for the back stack to serialize/restore correctly on iOS (unlike Android, there's no reflection-based
 * fallback there for open polymorphism), see
 * https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html.
 */
val destinationSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Destination.Loading::class, Destination.Loading.serializer())
            subclass(Destination.Logout::class, Destination.Logout.serializer())
            subclass(Destination.Login::class, Destination.Login.serializer())
            subclass(Destination.Main::class, Destination.Main.serializer())
            subclass(Destination.Settings::class, Destination.Settings.serializer())
            subclass(Destination.LendingDetails::class, Destination.LendingDetails.serializer())
            subclass(Destination.ItemTypeDetails::class, Destination.ItemTypeDetails.serializer())
            subclass(Destination.Admin.LendingManagement::class, Destination.Admin.LendingManagement.serializer())
            subclass(Destination.LendingSignUp::class, Destination.LendingSignUp.serializer())
            subclass(Destination.LendingCreation::class, Destination.LendingCreation.serializer())
            subclass(Destination.LendingMemoryEditor::class, Destination.LendingMemoryEditor.serializer())
            subclass(Destination.External.ResetPassword::class, Destination.External.ResetPassword.serializer())
        }
    }
}
