package org.centrexcursionistalcoi.app.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FCMRegistrationTokenEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.FCMRegistrationTokens
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.push.PushNotification
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.json.contains
import org.slf4j.LoggerFactory


object Push {
    private val logger = LoggerFactory.getLogger(Push::class.java)

    private var pushConfigured = false

    fun init() {
        if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
            logger.error("GOOGLE_APPLICATION_CREDENTIALS environment variable is not set. Push notifications will not work.")
            return
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()
        FirebaseApp.initializeApp(options)

        pushConfigured = true
        logger.info("Push notification service initialized successfully.")
    }

    private fun sendPushNotification(tokens: List<String>, data: Map<String, String>) {
        if (!pushConfigured) return

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

    fun sendAdminPushNotification(data: Map<String, String>) {
        val admins = try {
            Database {
                UserReferenceEntity.find { UserReferences.groups.contains(ADMIN_GROUP_NAME) }
            }
        } catch (_: UnsupportedByDialectException) {
            Database {
                UserReferenceEntity.all()
                    .filter { it.groups.contains(ADMIN_GROUP_NAME) }
            }
        }

        val tokens = Database {
            admins
                .flatMap { FCMRegistrationTokenEntity.find { FCMRegistrationTokens.user eq it.id } }
                .map { it.token }
        }
        sendPushNotification(tokens, data)
    }

    fun sendAdminPushNotification(notification: PushNotification) {
        sendAdminPushNotification(
            mapOf(
                "type" to notification.type,
                *notification.toMap().toList().toTypedArray()
            )
        )
    }
}
