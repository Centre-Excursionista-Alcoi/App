package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
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
import org.centrexcursionistalcoi.app.integration.FEMECV
import org.centrexcursionistalcoi.app.integration.femecv.FEMECVException
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.request.FileRequestData
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.routes.helper.handleIfModified
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import java.time.LocalDate

fun Route.profileRoutes() {
    get("/profile") {
        val session = getUserSessionOrFail() ?: return@get

        handleIfModified(UserReferenceEntity, session.sub) ?: return@get

        val reference = Database { UserReferenceEntity[session.sub] }
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
        assertContentType() ?: return@post

        var insuranceCompany: String? = null
        var policyNumber: String? = null
        var validFrom: String? = null
        var validTo: String? = null
        val document = FileRequestData()

        call.receiveMultipart().forEachPart { partData ->
            if (partData is PartData.FormItem) {
                when (partData.name) {
                    "insuranceCompany" -> insuranceCompany = partData.value
                    "policyNumber" -> policyNumber = partData.value
                    "validFrom" -> validFrom = partData.value
                    "validTo" -> validTo = partData.value
                    "document" -> document.populate(partData)
                }
            } else if (partData is PartData.FileItem) {
                when (partData.name) {
                    "document" -> document.populate(partData)
                }
            }
        }

        if (insuranceCompany.isNullOrBlank()) return@post call.respondError(Error.MissingArgument("insuranceCompany"))
        if (policyNumber.isNullOrBlank()) return@post call.respondError(Error.MissingArgument("policyNumber"))
        if (validFrom.isNullOrBlank()) return@post call.respondError(Error.MissingArgument("validFrom"))
        if (validTo.isNullOrBlank()) return@post call.respondError(Error.MissingArgument("validTo"))

        val validFromDate = try {
            LocalDate.parse(validFrom)
        } catch (_: DateTimeParseException) {
            return@post call.respondError(Error.InvalidArgument("validFrom", "Must be a valid date"))
        }
        val validToDate = try {
            LocalDate.parse(validTo)
        } catch (_: DateTimeParseException) {
            return@post call.respondError(Error.InvalidArgument("validTo", "Must be a valid date"))
        }

        val documentFile = document.takeIf { it.isNotEmpty() }?.newEntity()
        Database {
            UserInsuranceEntity.new {
                userSub = Database { UserReferenceEntity[session.sub] }
                this.insuranceCompany = insuranceCompany
                this.policyNumber = policyNumber
                this.validFrom = validFromDate
                this.validTo = validToDate
                this.document = documentFile
            }
        }

        call.respond(HttpStatusCode.NoContent)
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
        }
        userReference.updated()

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
    get("/profile/femecvSync/image/{year}") {
        val yearParam = call.parameters["year"] ?: return@get respondError(Error.MissingArgument("year"))
        val year = yearParam.toIntOrNull() ?: return@get respondError(Error.InvalidArgument("year", "Must be a valid year"))

        this::class.java.getResourceAsStream("/insurances/femecv/$year.png")?.use { stream ->
            call.respondBytesWriter(ContentType.Image.PNG) {
                stream.toByteReadChannel().copyTo(this)
            }
        } ?: call.respond(HttpStatusCode.NotFound)
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
