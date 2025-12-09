package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.toJavaLocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.toJavaUuid

class TestUsersRoutes: ApplicationTestBase() {
    @Test
    fun test_users_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/users")

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
                this.userSub = fakeUser
                this.sports = listOf(Sports.HIKING)
                this.phoneNumber = "123456789"
            }
            UserInsuranceEntity.new {
                this.userSub = fakeUser
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
                assertEquals(FakeAdminUser.FULL_NAME, user.fullName)
                assertEquals(FakeAdminUser.EMAIL, user.email)
                assertEquals(FakeAdminUser.GROUPS, user.groups)
                assertTrue(user.departments.isEmpty())
                assertEquals(null, user.lendingUser)
                assertTrue(user.insurances.isEmpty())
            }
            users[1].let { user ->
                assertEquals(FakeUser.SUB, user.sub)
                assertEquals(FakeUser.FULL_NAME, user.fullName)
                assertEquals(FakeUser.EMAIL, user.email)
                assertEquals(FakeUser.GROUPS, user.groups)

                assertEquals(2, user.departments.size)
                user.departments[0].let { dept ->
                    assertEquals(FakeUser.SUB, dept.userSub)
                    assertEquals("example 1", departments[dept.departmentId.toJavaUuid()]?.displayName)
                    assertTrue(dept.confirmed)
                }
                user.departments[1].let { dept ->
                    assertEquals(FakeUser.SUB, dept.userSub)
                    assertEquals("example 2", departments[dept.departmentId.toJavaUuid()]?.displayName)
                    assertFalse(dept.confirmed)
                }

                user.lendingUser?.let { lendingUser ->
                    assertEquals(FakeUser.SUB, lendingUser.sub)
                    assertEquals(listOf(Sports.HIKING), lendingUser.sports)
                    assertEquals("123456789", lendingUser.phoneNumber)
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

    @Test
    fun test_users_not_admin() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        client.get("/users").apply {
            assertError(Error.NotAnAdmin())
        }
    }
}
