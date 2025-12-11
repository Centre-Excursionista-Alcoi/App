package org.centrexcursionistalcoi.app.test

import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.entity.FCMRegistrationTokenEntity
import org.centrexcursionistalcoi.app.database.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.security.AES
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.slf4j.LoggerFactory

abstract class StubUser(val sub: String, val nif: String, val fullName: String, val email: String, val memberNumber: UInt, val groups: List<String>, val fcmToken: String) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    context(_: JdbcTransaction)
    fun provideEntity(): UserReferenceEntity {
        if (!AES.isInitialized()) AES.initForTests()

        return UserReferenceEntity.findById(sub) ?: UserReferenceEntity.new(sub) {
            nif = this@StubUser.nif
            fullName = this@StubUser.fullName
            email = this@StubUser.email
            groups = this@StubUser.groups
            memberNumber = this@StubUser.memberNumber
            password = ByteArray(0)

            logger.info("Created stub user entity: $sub - $nif - $fullName - $email - $groups")
        }.also { runBlocking { it.updated() } }
    }

    context(_: JdbcTransaction)
    fun provideMemberEntity(status: Member.Status = Member.Status.ACTIVE): MemberEntity {
        return MemberEntity.findById(memberNumber) ?: MemberEntity.new(memberNumber) {
            this.status = status
            this.fullName = this@StubUser.fullName
            this.nif = this@StubUser.nif
            this.email = this@StubUser.email

            logger.info("Created stub member entity: $memberNumber - $nif - $fullName - $email - $status")
        }
    }

    context(_: JdbcTransaction)
    fun provideEntityWithFCMToken(): FCMRegistrationTokenEntity {
        val entity = provideEntity()
        return FCMRegistrationTokenEntity.findById(fcmToken) ?: FCMRegistrationTokenEntity.new(fcmToken) { user = entity }
    }

    fun member(): Member = Member(
        memberNumber = memberNumber,
        fullName = fullName,
        nif = nif,
        email = email,
        status = Member.Status.ACTIVE,
    )

    fun data(): UserData = UserData(
        sub = sub,
        memberNumber = memberNumber,
        fullName = fullName,
        email = email,
        groups = groups,
        departments = emptyList(),
        lendingUser = null,
        insurances = emptyList(),
        isDisabled = false,
    )
}
