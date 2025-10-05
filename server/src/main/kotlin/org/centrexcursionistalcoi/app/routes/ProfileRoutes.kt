package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.time.format.DateTimeParseException

fun Route.profileRoutes() {
    get("/profile") {
        val session = getUserSessionOrFail() ?: return@get

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
            ProfileResponse(session.username, session.email, session.groups, departments, lendingUser, insurances)
        )
    }
    post("/profile/lendingSignUp") {
        val session = getUserSessionOrFail() ?: return@post

        val contentType = call.request.contentType()
        if (!contentType.match(ContentType.Application.FormUrlEncoded)) {
            call.respondText("Content-Type must be form url-encoded. It was: $contentType", status = HttpStatusCode.BadRequest)
            return@post
        }

        val existingUser = Database { LendingUserEntity.find { LendingUsers.userSub eq session.sub }.firstOrNull() }
        if (existingUser != null) {
            call.respondText("User already signed up", status = HttpStatusCode.Conflict)
            return@post
        }

        val parameters = call.receiveParameters()
        val fullName = parameters["fullName"]
        val nif = parameters["nif"]
        val phoneNumber = parameters["phoneNumber"]
        val sports = parameters["sports"]?.split(',')?.map(String::trim)?.map(Sports::valueOf)
        val address = parameters["address"]
        val postalCode = parameters["postalCode"]
        val city = parameters["city"]
        val province = parameters["province"]
        val country = parameters["country"]

        if (nif.isNullOrBlank()) return@post call.respondText("NIF is required", status = HttpStatusCode.BadRequest)
        if (phoneNumber.isNullOrBlank()) return@post call.respondText("Phone number is required", status = HttpStatusCode.BadRequest)
        if (fullName.isNullOrBlank()) return@post call.respondText("Full name is required", status = HttpStatusCode.BadRequest)
        if (sports.isNullOrEmpty()) return@post call.respondText("At least one sport must be selected", status = HttpStatusCode.BadRequest)
        if (address.isNullOrBlank()) return@post call.respondText("Address is required", status = HttpStatusCode.BadRequest)
        if (postalCode.isNullOrBlank()) return@post call.respondText("Postal code is required", status = HttpStatusCode.BadRequest)
        if (city.isNullOrBlank()) return@post call.respondText("City is required", status = HttpStatusCode.BadRequest)
        if (province.isNullOrBlank()) return@post call.respondText("Province is required", status = HttpStatusCode.BadRequest)
        if (country.isNullOrBlank()) return@post call.respondText("Country is required", status = HttpStatusCode.BadRequest)

        Database {
            LendingUserEntity.new {
                userSub = session.sub
                this.fullName = fullName
                this.nif = nif
                this.phoneNumber = phoneNumber
                this.sports = sports
                this.address = address
                this.postalCode = postalCode
                this.city = city
                this.province = province
                this.country = country
            }
        }

        call.respondText("OK", status = HttpStatusCode.Created)
    }
    get("/profile/insurances") {
        val session = getUserSessionOrFail() ?: return@get

        val insurances = Database { UserInsuranceEntity.find { UserInsurances.userSub eq session.sub }.map { it.toData() } }
        call.respond(insurances)
    }
    post("/profile/insurances") {
        val session = getUserSessionOrFail() ?: return@post

        val contentType = call.request.contentType()
        if (!contentType.match(ContentType.Application.FormUrlEncoded)) {
            call.respondText("Content-Type must be form url-encoded. It was: $contentType", status = HttpStatusCode.BadRequest)
            return@post
        }

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
                userSub = session.sub
                this.insuranceCompany = insuranceCompany
                this.policyNumber = policyNumber
                this.validFrom = validFromDate
                this.validTo = validToDate
            }
        }

        call.respondText("OK", status = HttpStatusCode.Created)
    }
}
