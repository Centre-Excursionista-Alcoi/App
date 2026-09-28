package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
import kotlinx.datetime.toJavaLocalDate
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.FCMRegistrationTokenEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
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
import org.centrexcursionistalcoi.app.request.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.request.LendingSignUpRequest
import org.centrexcursionistalcoi.app.request.LinkFEMECVRequest
import org.centrexcursionistalcoi.app.request.MissingPartException
import org.centrexcursionistalcoi.app.request.RegisterFCMTokenRequest
import org.centrexcursionistalcoi.app.request.RevokeFCMTokenRequest
import org.centrexcursionistalcoi.app.request.receiveJson
import org.centrexcursionistalcoi.app.request.receiveRequestWithFiles
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.routes.helper.handleIfModified
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSessionOrFail
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.slf4j.LoggerFactory
import kotlin.time.toKotlinInstant

private val logger = LoggerFactory.getLogger("ProfileRoutes")

fun Route.profileRoutes() {
    get("/profile") {
        val session = getUserSessionOrFail() ?: return@get

        handleIfModified(UserReferenceEntity, session.sub) ?: return@get

        val reference = Database { UserReferenceEntity[session.sub] }
        try {
            if (reference.femecvUsername != null && reference.femecvPassword != null) {
                val lastSync = reference.femecvLastSync
                if (lastSync == null || (now() - lastSync).inWholeDays >= FEMECV.REFRESH_EVERY_DAYS) {
                    reference.refreshFEMECVData()
                }
            }
        } catch (e: FEMECVException) {
            call.response.header("CEA-FEMECV-Error", e.message ?: "Unknown")
        }

        val departments = Database {
            DepartmentMemberEntity.find {
                (DepartmentMembers.userSub eq session.sub) and (DepartmentMembers.confirmed eq true)
            }.map { it.department.id.value }
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
                femecvLastSync = reference.femecvLastSync,
            )
        )
    }
    post("/profile/lendingSignUp") {
        val session = getUserSessionOrFail() ?: return@post

        val existingUser = Database { LendingUserEntity.find { LendingUsers.userSub eq session.sub }.firstOrNull() }
        if (existingUser != null) {
            call.respondError(Error.UserAlreadyRegisteredForLending())
            return@post
        }

        val request = receiveJson(LendingSignUpRequest.serializer()) ?: return@post

        if (request.phoneNumber.isBlank()) return@post call.respondError(Error.MissingArgument("phoneNumber"))
        if (request.sports.isEmpty()) return@post call.respondError(Error.MissingArgument("sports"))

        val userReference = Database { UserReferenceEntity[session.sub] }
        Database {
            LendingUserEntity.new {
                userSub = userReference
                this.phoneNumber = request.phoneNumber
                this.sports = request.sports
            }
        }
        userReference.updated()

        call.respond(HttpStatusCode.Created)
    }
    get("/profile/insurances") {
        val session = getUserSessionOrFail() ?: return@get

        val insurances = Database { UserInsuranceEntity.find { UserInsurances.userSub eq session.sub }.map { it.toData() } }
        call.respond(insurances)
    }
    post("/profile/insurances") {
        val session = getUserSessionOrFail() ?: return@post

        val received = receiveRequestWithFiles(CreateInsuranceRequest.serializer()) ?: return@post
        val request = received.request

        if (request.insuranceCompany.isBlank()) return@post call.respondError(Error.MissingArgument("insuranceCompany"))
        if (request.policyNumber.isBlank()) return@post call.respondError(Error.MissingArgument("policyNumber"))

        val userReference = Database { UserReferenceEntity[session.sub] }
        try {
            received.withUploads {
                Database {
                    val documentFiles = request.documents.filterNot { it.isEmpty() }.map { document ->
                        FileEntity.newFrom(
                            document,
                            rules = FileReadWriteRules(readUsers = listOf(session.sub), readGroups = listOf(ADMIN_GROUP_NAME)),
                        )
                    }
                    UserInsuranceEntity.new {
                        userSub = userReference
                        this.insuranceCompany = request.insuranceCompany
                        this.policyNumber = request.policyNumber
                        this.validFrom = request.validFrom
                        this.validTo = request.validTo
                    }.addDocuments(documentFiles)
                }
            }
        } catch (e: MissingPartException) {
            logger.error("Insurance request refers to a missing part", e)
            return@post call.respondError(Error.MalformedRequest())
        }
        userReference.updated()

        call.respond(HttpStatusCode.NoContent)
    }
    post("/profile/femecvSync") {
        val session = getUserSessionOrFail() ?: return@post

        val request = receiveJson(LinkFEMECVRequest.serializer()) ?: return@post
        val username = request.username
        val password = request.password

        if (username.isBlank() || password.isBlank()) return@post respondError(Error.FEMECVMissingCredentials())

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

        val request = receiveJson(RegisterFCMTokenRequest.serializer()) ?: return@post
        val token = request.token
        val deviceId = request.deviceId

        if (token.isBlank()) return@post respondError(Error.FCMTokenIsRequired())

        Database {
            // Only add the token if it doesn't already exist
            if (FCMRegistrationTokenEntity.findById(token) == null) {
                val reference = UserReferenceEntity[session.sub]
                reference.addFCMRegistrationToken(token, deviceId)
            }
        }

        call.respond(HttpStatusCode.Created)
    }
    // Delete by device id
    delete("/profile/fcmToken") {
        val session = getUserSessionOrFail() ?: return@delete

        val request = receiveJson(RevokeFCMTokenRequest.serializer()) ?: return@delete
        val deviceId = request.deviceId

        if (deviceId.isBlank()) return@delete respondError(Error.DeviceIdIsRequired())

        Database {
            val reference = UserReferenceEntity[session.sub]
            FCMRegistrationTokenEntity.find {
                (FCMRegistrationTokens.deviceId eq deviceId) and (FCMRegistrationTokens.user eq reference.id)
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
