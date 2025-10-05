package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.toJavaLocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TestUsersRoutes: ApplicationTestBase() {
    @Test
    fun test_users_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/users")

    @Test
    fun test_users_notAdmin() = ProvidedRouteTests.test_loggedIn_notAdmin("/users")

    @Test
    fun test_users() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            FakeAdminUser.provideEntity()
            val fakeUser = FakeUser.provideEntity()

            val (department1, department2) = transaction {
                DepartmentEntity.new { this.displayName = "example 1" } to DepartmentEntity.new { this.displayName = "example 2" }
            }

            DepartmentMemberEntity.new {
                this.department = department1
                this.userSub = fakeUser.sub
                this.confirmed = true
            }
            DepartmentMemberEntity.new {
                this.department = department2
                this.userSub = fakeUser.sub
                this.confirmed = false
            }
            LendingUserEntity.new {
                this.userSub = fakeUser.sub
                this.fullName = "Full Name"
                this.nif = "NIF123456"
                this.sports = listOf(Sports.HIKING)
                this.phoneNumber = "123456789"
                this.address = "Some Address"
                this.postalCode = "12345"
                this.city = "City"
                this.country = "Country"
                this.province = "Province"
            }
            UserInsuranceEntity.new {
                this.userSub = fakeUser.sub
                this.insuranceCompany = "Insurance Co"
                this.policyNumber = "POL123456"
                this.validFrom = LocalDate.of(2025, 1, 1)
                this.validTo = LocalDate.of(2025, 12, 31)
            }
        }
    ) {
        client.get("/users").apply {
            assertStatusCode(HttpStatusCode.OK)

            val departments = Database { DepartmentEntity.all().associate { it.id.value to it.toData() } }

            val users = bodyAsJson(ListSerializer(UserData.serializer()))
            assertEquals(2, users.size)
            users[0].let { user ->
                assertEquals(FakeAdminUser.SUB, user.sub)
                assertEquals(FakeAdminUser.USERNAME, user.username)
                assertEquals(FakeAdminUser.EMAIL, user.email)
                assertEquals(FakeAdminUser.GROUPS, user.groups)
                assertTrue(user.departments.isEmpty())
                assertEquals(null, user.lendingUser)
                assertTrue(user.insurances.isEmpty())
            }
            users[1].let { user ->
                assertEquals(FakeUser.SUB, user.sub)
                assertEquals(FakeUser.USERNAME, user.username)
                assertEquals(FakeUser.EMAIL, user.email)
                assertEquals(FakeUser.GROUPS, user.groups)

                assertEquals(2, user.departments.size)
                user.departments[0].let { dept ->
                    assertEquals(FakeUser.SUB, dept.userSub)
                    assertEquals("example 1", departments[dept.departmentId]?.displayName)
                    assertTrue(dept.confirmed)
                }
                user.departments[1].let { dept ->
                    assertEquals(FakeUser.SUB, dept.userSub)
                    assertEquals("example 2", departments[dept.departmentId]?.displayName)
                    assertFalse(dept.confirmed)
                }

                user.lendingUser?.let { lendingUser ->
                    assertEquals(FakeUser.SUB, lendingUser.sub)
                    assertEquals("Full Name", lendingUser.fullName)
                    assertEquals("NIF123456", lendingUser.nif)
                    assertEquals(listOf(Sports.HIKING), lendingUser.sports)
                    assertEquals("123456789", lendingUser.phoneNumber)
                    assertEquals("Some Address", lendingUser.address)
                    assertEquals("12345", lendingUser.postalCode)
                    assertEquals("City", lendingUser.city)
                    assertEquals("Country", lendingUser.country)
                    assertEquals("Province", lendingUser.province)
                } ?: error("Lending user should not be null")

                assertEquals(1, user.insurances.size)
                user.insurances[0].let { insurance ->
                    assertEquals(FakeUser.SUB, insurance.userSub)
                    assertEquals("Insurance Co", insurance.insuranceCompany)
                    assertEquals("POL123456", insurance.policyNumber)
                    assertEquals(LocalDate.of(2025, 1, 1), insurance.validFrom.toJavaLocalDate())
                    assertEquals(LocalDate.of(2025, 12, 31), insurance.validTo.toJavaLocalDate())
                }
            }
        }
    }
}
