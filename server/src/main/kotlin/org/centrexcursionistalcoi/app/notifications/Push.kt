package org.centrexcursionistalcoi.app.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FCMRegistrationTokenEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.op.ValueInStringArrayOp
import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.notifications.Push.disable
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.push.PushNotification
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.slf4j.LoggerFactory
import java.util.*

object Push {
    private val logger = LoggerFactory.getLogger(Push::class.java)

    @VisibleForTesting
    var pushFCMConfigured = false

    private val notificationFlow = MutableSharedFlow<LocalNotification>()

    var disable: Boolean = false

    class LocalNotification(
        val notification: PushNotification,
        val userSub: String? = null,
        val includeAdmins: Boolean = false,
    )

    fun initFCM() {
        if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
            logger.error("GOOGLE_APPLICATION_CREDENTIALS environment variable is not set. Push notifications will not work.")
            return
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()
        FirebaseApp.initializeApp(options)

        pushFCMConfigured = true
        logger.info("Push notification service initialized successfully.")
    }

    /**
     * Sends a push notification asynchronously.
     *
     * This function launches a coroutine in the IO dispatcher to execute the provided suspend function [block].
     *
     * If push notifications are disabled (i.e., [disable] is true), the function returns immediately without executing the block.
     *
     * @param block The suspend function that contains the push notification logic.
     */
    fun launch(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.IO).launch {
        if (disable) return@launch
        block()
    }

    fun flow(session: UserSession) = notificationFlow
        .filter { notification ->
            if (notification.includeAdmins && session.isAdmin()) {
                // If the notification includes admins, and the session is an admin, send it
                true
            } else if (notification.userSub != null) {
                // Otherwise, if the userSub is specified, check if it matches
                notification.userSub == session.sub
            } else {
                // No userSub specified, and not for admins, don't send
                false
            }
        }
        .map { it.notification }

    @VisibleForTesting
    internal fun sendFCMPushNotification(tokens: Set<String>, data: Map<String, String>) {
        if (!pushFCMConfigured) return
        if (tokens.isEmpty()) {
            logger.debug("Won't send FCM notification because tokens list is empty.")
            return
        }

        logger.debug("Sending push notification to {} devices with data: {}", tokens.size, data)

        val message = MulticastMessage.builder()
            .putAllData(data)
            .addAllTokens(tokens)
            .build()
        val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)
        if (response.failureCount > 0) {
            val failedTokens = mutableListOf<String>()
            for ((i) in response.responses.withIndex()) {
                failedTokens += tokens.elementAt(i)
            }

            logger.warn("Failed to send push notifications to the following tokens: $failedTokens")
        }

        logger.info("Push notifications sent successfully to ${response.successCount} devices.")
    }

    context(_ : JdbcTransaction)
    private fun fetchTokens(predicate: () -> Op<Boolean>): Set<String> {
        // Join UserReferences and Tokens
        return (UserReferences innerJoin FCMRegistrationTokens)
            // Only select the token
            .select(FCMRegistrationTokens.token)
            // Of admin users
            .where(predicate)
            // Extract the token value
            .map { it[FCMRegistrationTokens.token].value }
            .toSet()
    }

    @VisibleForTesting
    internal fun sendAdminFCMPushNotification(data: Map<String, String>) {
        if (!pushFCMConfigured) return

        val tokens = Database {
            fetchTokens { ValueInStringArrayOp(ADMIN_GROUP_NAME, UserReferences.groups) }
        }
        sendFCMPushNotification(tokens, data)
    }

    @VisibleForTesting
    internal suspend fun sendLocalPushNotification(
        notification: PushNotification,
        userSub: String? = null,
        includeAdmins: Boolean = false,
    ) {
        notificationFlow.emit(
            LocalNotification(
                notification = notification,
                userSub = userSub,
                includeAdmins = includeAdmins,
            )
        )
        logger.info("Local push notification sent: type=${notification.type}, userSub=$userSub, includeAdmins=$includeAdmins")
    }

    suspend fun sendAdminPushNotification(notification: PushNotification) {
        sendLocalPushNotification(
            notification = notification,
            includeAdmins = true,
        )

        if (pushFCMConfigured) {
            sendAdminFCMPushNotification(
                mapOf(
                    "type" to notification.type,
                    *notification.toMap().toList().toTypedArray()
                )
            )
        }
    }

    suspend fun sendPushNotification(userSub: String, notification: PushNotification, includeAdmins: Boolean = true) {
        sendLocalPushNotification(
            notification = notification,
            userSub = userSub,
            includeAdmins = includeAdmins,
        )

        if (pushFCMConfigured) {
            val tokens = Database {
                // Get all the tokens for the given sub
                FCMRegistrationTokenEntity.find { FCMRegistrationTokens.user eq userSub }.map { it.token.value }
            }.toMutableSet()

            if (includeAdmins) {
                tokens += Database {
                    // if includeAdmins=true, also include admins that have not already been included
                    fetchTokens { ValueInStringArrayOp(ADMIN_GROUP_NAME, UserReferences.groups) and (FCMRegistrationTokens.user neq userSub) }
                }
            }

            sendFCMPushNotification(tokens, notification.toMap())
        }
    }

    suspend fun sendPushNotification(
        reference: UserReferenceEntity,
        notification: PushNotification,
        includeAdmins: Boolean = true
    ) {
        sendPushNotification(
            userSub = reference.sub.value,
            notification = notification,
            includeAdmins = includeAdmins,
        )
    }

    suspend fun sendPushNotificationToDepartment(
        notification: PushNotification,
        departmentId: UUID,
        includeAdmins: Boolean = true,
    ) {
        val references = Database {
            DepartmentEntity.findById(departmentId)
                ?.confirmedMembers
                ?.map { it.userReference.id.value }
                .orEmpty()
        }
        for (reference in references) {
            sendLocalPushNotification(
                notification = notification,
                userSub = reference,
                includeAdmins = includeAdmins,
            )
        }

        if (pushFCMConfigured) {
            val tokens = Database {
                FCMRegistrationTokenEntity.find { FCMRegistrationTokens.user inList references }.map { it.token.value }
            }.toMutableSet()
            if (includeAdmins) {
                val adminReferences = Database {
                    UserReferenceEntity.all()
                        // Filter admins, and exclude the department members
                        .filter { it.groups.contains(ADMIN_GROUP_NAME) && !references.contains(it.sub.value) }
                }
                val adminTokens = Database {
                    adminReferences.flatMap { FCMRegistrationTokenEntity.find { FCMRegistrationTokens.user eq it.id } }
                        .map { it.token.value }
                }
                tokens += adminTokens
            }

            sendFCMPushNotification(tokens, notification.toMap())
        }
    }

    suspend fun sendPushNotificationToAll(notification: PushNotification) {
        val allReferences = Database {
            UserReferenceEntity.all().toList()
        }
        for (reference in allReferences) {
            sendLocalPushNotification(
                notification = notification,
                userSub = reference.sub.value,
            )
        }

        if (pushFCMConfigured) {
            val tokens = Database {
                FCMRegistrationTokenEntity.all().map { it.token.value }.toSet()
            }

            sendFCMPushNotification(tokens, notification.toMap())
        }
    }
}
