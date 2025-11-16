package org.centrexcursionistalcoi.app.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FCMRegistrationTokenEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.push.PushNotification
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.v1.core.eq
import org.slf4j.LoggerFactory

object Push {
    private val logger = LoggerFactory.getLogger(Push::class.java)

    private var pushFCMConfigured = false

    private val notificationFlow = MutableSharedFlow<LocalNotification>()

    class LocalNotification(
        val notification: PushNotification,
        val userSub: String? = null,
        val includeAdmins: Boolean = false,
    )

    fun init() {
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

    private fun sendFCMPushNotification(tokens: List<String>, data: Map<String, String>) {
        if (!pushFCMConfigured) return

        logger.debug("Sending push notification to {} devices with data: {}", tokens.size, data)

        val message = MulticastMessage.builder()
            .putAllData(data)
            .addAllTokens(tokens)
            .build()
        val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)
        if (response.failureCount > 0) {
            val failedTokens = mutableListOf<String>()
            for ((i) in response.responses.withIndex()) {
                failedTokens += tokens[i]
            }

            logger.warn("Failed to send push notifications to the following tokens: $failedTokens")
        }

        logger.info("Push notifications sent successfully to ${response.successCount} devices.")
    }

    private fun sendAdminFCMPushNotification(data: Map<String, String>) {
        if (!pushFCMConfigured) return

        val admins = Database {
            UserReferenceEntity.all().filter { it.groups.contains(ADMIN_GROUP_NAME) }
        }

        val tokens = Database {
            admins.flatMap { FCMRegistrationTokenEntity.find { FCMRegistrationTokens.user eq it.id } }
                .map { it.token.value }
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

    suspend fun sendPushNotification(reference: UserReferenceEntity, notification: PushNotification, includeAdmins: Boolean = true) {
        sendLocalPushNotification(
            notification = notification,
            userSub = reference.sub.value,
            includeAdmins = includeAdmins,
        )

        if (pushFCMConfigured) {
            var tokens = Database {
                FCMRegistrationTokenEntity.find { FCMRegistrationTokens.user eq reference.id }.map { it.token.value }
            }
            if (includeAdmins) {
                val adminTokens = Database {
                    UserReferenceEntity.all()
                        // Filter admins, and exclude the given reference
                        .filter { it.groups.contains(ADMIN_GROUP_NAME) && it.sub != reference.sub }
                        .flatMap { FCMRegistrationTokenEntity.find { FCMRegistrationTokens.user eq it.id } }
                        .map { it.token.value }
                }
                tokens = tokens + adminTokens
            }

            sendFCMPushNotification(
                tokens,
                mapOf(
                    "type" to notification.type,
                    *notification.toMap().toList().toTypedArray()
                )
            )
        }
    }
}
