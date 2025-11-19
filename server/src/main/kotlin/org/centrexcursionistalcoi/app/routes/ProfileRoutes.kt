package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.time.toKotlinInstant
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.FCMRegistrationTokenEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.ifModifiedSince
import org.centrexcursionistalcoi.app.integration.FEMECV
import org.centrexcursionistalcoi.app.integration.femecv.FEMECVException
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq

fun Route.profileRoutes() {
    get("/profile") {
        val session = getUserSessionOrFail() ?: return@get

        val reference = Database { UserReferenceEntity[session.sub] }

        // Handle If-Modified-Since header
        val ifModifiedSince = call.request.ifModifiedSince()?.toInstant()
        if (ifModifiedSince != null) {
            val refLastUpdate = Database { reference.lastUpdate }
            if (refLastUpdate <= ifModifiedSince) {
                call.respond(HttpStatusCode.NotModified)
                return@get
            }
        }

        try {
            if (reference.femecvUsername != null && reference.femecvPassword != null) {
                val lastSync = reference.femecvLastSync
                if (lastSync == null || lastSync.until(now(), ChronoUnit.DAYS) >= FEMECV.REFRESH_EVERY_DAYS) {
                    reference.refreshFEMECVData()
                }
            }
        } catch (e: FEMECVException) {
            call.response.header("CEA-FEMECV-Error", e.message ?: "Unknown")
        }

        val departments = Database {
            DepartmentMemberEntity.find {
                (DepartmentMembers.userSub eq session.sub) and (DepartmentMembers.confirmed eq true)
            }.map { it.department.id.value.toKotlinUuid() }
        }
        val lendingUser = Database {
            LendingUserEntity.find { LendingUsers.userSub eq session.sub }.firstOrNull()?.toData()
        }

        val insurances = Database { UserInsuranceEntity.find { UserInsurances.userSub eq session.sub }.map { it.toData() } }

        call.respond(
            ProfileResponse(
                sub = session.sub,
                fullName = session.fullName,
                memberNumber = reference.memberNumber,
                email = session.email,
                groups = session.groups,
                departments = departments,
                lendingUser = lendingUser,
                insurances = insurances,
                femecvSyncEnabled = reference.femecvUsername != null && reference.femecvPassword != null,
                femecvLastSync = reference.femecvLastSync?.toKotlinInstant(),
            )
        )
    }
    post("/profile/lendingSignUp") {
        val session = getUserSessionOrFail() ?: return@post

        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val existingUser = Database { LendingUserEntity.find { LendingUsers.userSub eq session.sub }.firstOrNull() }
        if (existingUser != null) {
            call.respondError(Error.UserAlreadyRegisteredForLending())
            return@post
        }

        val parameters = call.receiveParameters()
        val phoneNumber = parameters["phoneNumber"]
        val sports = parameters["sports"]?.split(',')?.map(String::trim)?.map(Sports::valueOf)

        if (phoneNumber.isNullOrBlank()) return@post call.respondError(Error.MissingArgument("phoneNumber"))
        if (sports.isNullOrEmpty()) return@post call.respondError(Error.MissingArgument("sports"))

        Database {
            LendingUserEntity.new {
                userSub = Database { UserReferenceEntity[session.sub].id }
                this.phoneNumber = phoneNumber
                this.sports = sports
            }
        }

        call.respond(HttpStatusCode.Created)
    }
    get("/profile/insurances") {
        val session = getUserSessionOrFail() ?: return@get

        val insurances = Database { UserInsuranceEntity.find { UserInsurances.userSub eq session.sub }.map { it.toData() } }
        call.respond(insurances)
    }
    post("/profile/insurances") {
        val session = getUserSessionOrFail() ?: return@post

        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val insuranceCompany = parameters["insuranceCompany"]
        val policyNumber = parameters["policyNumber"]
        val validFrom = parameters["validFrom"]
        val validTo = parameters["validTo"]

        if (insuranceCompany.isNullOrBlank()) return@post call.respondText("Insurance company is required", status = HttpStatusCode.BadRequest)
        if (policyNumber.isNullOrBlank()) return@post call.respondText("Policy number is required", status = HttpStatusCode.BadRequest)
        if (validFrom.isNullOrBlank()) return@post call.respondText("Valid until date is required", status = HttpStatusCode.BadRequest)
        if (validTo.isNullOrBlank()) return@post call.respondText("Valid from date is required", status = HttpStatusCode.BadRequest)

        val validFromDate = try {
            java.time.LocalDate.parse(validFrom)
        } catch (_: DateTimeParseException) {
            return@post call.respondText("Valid from date is not valid", status = HttpStatusCode.BadRequest)
        }
        val validToDate = try {
            java.time.LocalDate.parse(validTo)
        } catch (_: DateTimeParseException) {
            return@post call.respondText("Valid until date is not valid", status = HttpStatusCode.BadRequest)
        }

        Database {
            UserInsuranceEntity.new {
                userSub = Database { UserReferenceEntity[session.sub] }
                this.insuranceCompany = insuranceCompany
                this.policyNumber = policyNumber
                this.validFrom = validFromDate
                this.validTo = validToDate
            }
        }

        call.respondText("OK", status = HttpStatusCode.Created)
    }
    post("/profile/femecvSync") {
        val session = getUserSessionOrFail() ?: return@post

        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val username = parameters["username"]
        val password = parameters["password"]

        if (username.isNullOrBlank() || password.isNullOrBlank()) return@post respondError(Error.FEMECVMissingCredentials())

        try {
            FEMECV.login(username, password)
        } catch (e: FEMECVException) {
            return@post call.respondText("FEMECV login failed: ${e.message}", status = HttpStatusCode.Unauthorized)
        }

        val userReference = Database { UserReferenceEntity[session.sub] }

        Database {
            userReference.femecvUsername = username
            userReference.femecvPassword = password

            userReference.lastUpdate = now()
        }

        try {
            userReference.refreshFEMECVData()
        } catch (e: FEMECVException) {
            return@post call.respondText("FEMECV data sync failed: ${e.message}", status = HttpStatusCode.InternalServerError)
        }

        call.respondText("FEMECV account linked and data synchronized successfully", status = HttpStatusCode.OK)
    }
    delete("/profile/femecvSync") {
        val session = getUserSessionOrFail() ?: return@delete

        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@delete

        val userReference = Database { UserReferenceEntity[session.sub] }

        // Delete FEMECV-linked insurances
        Database {
            UserInsuranceEntity.find { (UserInsurances.userSub eq session.sub) and (UserInsurances.femecvLicense neq null) }
                .forEach { entity ->
                    entity.delete()
                }
        }

        // Remove FEMECV credentials
        Database {
            userReference.femecvUsername = null
            userReference.femecvPassword = null
        }

        call.respond(HttpStatusCode.NoContent)
    }
    post("/profile/fcmToken") {
        val session = getUserSessionOrFail() ?: return@post

        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@post

        val parameters = call.receiveParameters()
        val token = parameters["token"]
        val deviceId = parameters["deviceId"]

        if (token.isNullOrBlank()) return@post respondError(Error.FCMTokenIsRequired())

        Database {
            val reference = UserReferenceEntity[session.sub]
            reference.addFCMRegistrationToken(token, deviceId)
        }

        call.respond(HttpStatusCode.Created)
    }
    // Delete by device id
    delete("/profile/fcmToken") {
        val session = getUserSessionOrFail() ?: return@delete

        assertContentType(ContentType.Application.FormUrlEncoded) ?: return@delete

        val parameters = call.receiveParameters()
        val token = parameters["deviceId"]

        if (token.isNullOrBlank()) return@delete respondError(Error.DeviceIdIsRequired())

        Database {
            val reference = UserReferenceEntity[session.sub]
            FCMRegistrationTokenEntity.find {
                (FCMRegistrationTokens.deviceId eq token) and (FCMRegistrationTokens.user eq reference.id)
            }.forEach { it.delete() }
        }

        call.respond(HttpStatusCode.NoContent)
    }
    // Delete by token id
    delete("/profile/fcmToken/{token}") {
        val session = getUserSessionOrFail() ?: return@delete

        val token = call.parameters["token"]!!

        Database {
            FCMRegistrationTokenEntity.findById(token)
                // Make sure the token belongs to the user
                ?.takeIf { it.user.sub.value == session.sub }
                // Delete the token
                ?.delete()
        }

        call.respond(HttpStatusCode.NoContent)
    }
}
