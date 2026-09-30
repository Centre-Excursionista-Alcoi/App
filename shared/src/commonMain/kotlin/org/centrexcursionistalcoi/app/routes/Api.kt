package org.centrexcursionistalcoi.app.routes

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

/**
 * The server's API, as type-safe [Resource]s shared by the server's routes and the app's requests.
 */
object Api {
    @Resource("/auth")
    class Auth {
        @Resource("login")
        class Login(val parent: Auth = Auth())

        @Resource("webauthn/verify")
        class WebAuthnVerify(val parent: Auth = Auth())

        @Resource("refresh")
        class Refresh(val parent: Auth = Auth())

        @Resource("logout")
        class Logout(val parent: Auth = Auth())
    }

    @Resource("/register")
    class Register {
        /** Emails the code that proves the email is the user's, needed to register. */
        @Resource("verification")
        class Verification(val parent: Register = Register())

        /** Registers an account that signs in with a passkey. */
        @Resource("passkey")
        class Passkey(val parent: Register = Register()) {
            @Resource("options")
            class Options(val parent: Passkey = Passkey())
        }
    }

    @Resource("/lost_password")
    class LostPassword(@SerialName("redirect_to") val redirectTo: String? = null)

    /** The password reset web page (`GET`), and the reset itself (`POST`). The parameters are only used by the page. */
    @Resource("/reset_password")
    class ResetPassword(
        @SerialName("request_id") val requestId: String? = null,
        val error: String? = null,
        val success: String? = null,
    )

    @Resource("/delete_account")
    class DeleteAccount

    @Resource("/generate-restore-challenge")
    class GenerateRestoreChallenge

    @Resource("/generate-auth-challenge")
    class GenerateAuthChallenge

    @Resource("/register-restore-key")
    class RegisterRestoreKey

    @Resource("/info")
    class Info

    @Resource("/download")
    class Download {
        @Resource("{uuid}")
        class Id(val uuid: String, val parent: Download = Download())
    }

    @Resource("/profile")
    class Profile {
        /** How the user signs in: their password and passkeys. */
        @Resource("security")
        class Security(val parent: Profile = Profile())

        @Resource("passkeys")
        class Passkeys(val parent: Profile = Profile()) {
            @Resource("options")
            class Options(val parent: Passkeys = Passkeys())

            @Resource("{id}")
            class Id(val id: String, val parent: Passkeys = Passkeys())
        }

        @Resource("password")
        class Password(val parent: Profile = Profile())

        @Resource("lendingSignUp")
        class LendingSignUp(val parent: Profile = Profile())

        @Resource("insurances")
        class Insurances(val parent: Profile = Profile())

        @Resource("femecvSync")
        class FEMECVSync(val parent: Profile = Profile()) {
            @Resource("image")
            class Image(val parent: FEMECVSync = FEMECVSync()) {
                @Resource("{year}")
                class ByYear(val year: Int, val parent: Image = Image())
            }
        }

        @Resource("fcmToken")
        class FCMToken(val parent: Profile = Profile()) {
            @Resource("{token}")
            class ByToken(val token: String, val parent: FCMToken = FCMToken())
        }
    }

    @Resource("/departments")
    class Departments {
        @Resource("{id}")
        class Id(val id: String, val parent: Departments = Departments()) {
            @Resource("join")
            class Join(val parent: Id)

            @Resource("leave")
            class Leave(val parent: Id) {
                @Resource("{sub}")
                class Member(val sub: String, val parent: Leave)
            }

            @Resource("members")
            class Members(val parent: Id) {
                @Resource("{memberId}/roles")
                class Roles(val memberId: String, val parent: Members)
            }

            @Resource("confirm/{requestId}")
            class Confirm(val requestId: String, val parent: Id)

            @Resource("deny/{requestId}")
            class Deny(val requestId: String, val parent: Id)

            @Resource("qualifications")
            class Qualifications(val parent: Id)

            /** Only the members whose name contains [q], if given, and at most [limit] of them. */
            @Resource("roster")
            class Roster(val parent: Id, val q: String? = null, val limit: String? = null)
        }

        companion object {
            val resources = EntityResources(serializer(), Id.serializer(), Departments(), ::Id, Id::id)
        }
    }

    @Resource("/events")
    class Events {
        @Resource("{id}")
        class Id(val id: String, val parent: Events = Events()) {
            @Resource("confirm")
            class Confirm(val parent: Id)

            @Resource("reject")
            class Reject(val parent: Id)
        }

        @Resource("calendar")
        class Calendar(val parent: Events = Events())

        companion object {
            val resources = EntityResources(serializer(), Id.serializer(), Events(), ::Id, Id::id)
        }
    }

    @Resource("/posts")
    class Posts {
        @Resource("{id}")
        class Id(val id: String, val parent: Posts = Posts())

        companion object {
            val resources = EntityResources(serializer(), Id.serializer(), Posts(), ::Id, Id::id)
        }
    }

    @Resource("/inventory")
    class Inventory {
        @Resource("types")
        class Types(val parent: Inventory = Inventory()) {
            @Resource("{id}")
            class Id(val id: String, val parent: Types = Types()) {
                @Resource("allocate")
                class Allocate(val parent: Id)
            }

            companion object {
                val resources = EntityResources(serializer(), Id.serializer(), Types(), { Id(it) }, Id::id)
            }
        }

        @Resource("items")
        class Items(val parent: Inventory = Inventory()) {
            @Resource("{id}")
            class Id(val id: String, val parent: Items = Items())

            companion object {
                val resources = EntityResources(serializer(), Id.serializer(), Items(), { Id(it) }, Id::id)
            }
        }

        @Resource("lendings")
        class Lendings(val parent: Inventory = Inventory()) {
            @Resource("{id}")
            class Id(val id: String, val parent: Lendings = Lendings()) {
                @Resource("cancel")
                class Cancel(val parent: Id)

                @Resource("confirm")
                class Confirm(val parent: Id)

                @Resource("pickup")
                class Pickup(val parent: Id)

                @Resource("return")
                class Return(val parent: Id)

                @Resource("skip_memory")
                class SkipMemory(val parent: Id)
            }

            companion object {
                val resources = EntityResources(serializer(), Id.serializer(), Lendings(), { Id(it) }, Id::id)
            }
        }
    }

    @Resource("/memories")
    class Memories {
        @Resource("{id}")
        class Id(val id: String, val parent: Memories = Memories())

        companion object {
            val resources = EntityResources(serializer(), Id.serializer(), Memories(), ::Id, Id::id)
        }
    }

    @Resource("/users")
    class Users {
        @Resource("{sub}")
        class Sub(val sub: String, val parent: Users = Users()) {
            @Resource("promote")
            class Promote(val parent: Sub)
        }

        companion object {
            val resources = EntityResources(serializer(), Sub.serializer(), Users(), ::Sub, Sub::sub)
        }
    }

    @Resource("/members")
    class Members {
        /** Not served: members can only be listed. Only here for [resources]. */
        @Resource("{id}")
        class Id(val id: String, val parent: Members = Members())

        companion object {
            val resources = EntityResources(serializer(), Id.serializer(), Members(), ::Id, Id::id)
        }
    }

    @Resource("/qualifications")
    class Qualifications {
        @Resource("{id}")
        class Id(val id: String, val parent: Qualifications = Qualifications()) {
            @Resource("grants")
            class Grants(val parent: Id) {
                @Resource("{sub}")
                class Sub(val sub: String, val parent: Grants)
            }
        }
    }
}
