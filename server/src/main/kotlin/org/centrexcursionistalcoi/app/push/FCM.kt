package org.centrexcursionistalcoi.app.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import java.io.File
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.SessionsDatabase
import org.centrexcursionistalcoi.app.database.entity.notification.Notification
import org.centrexcursionistalcoi.app.push.payload.PushPayload
import org.centrexcursionistalcoi.app.serverJson
import org.slf4j.LoggerFactory

object FCM {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private lateinit var messaging: FirebaseMessaging

    fun initialize(serviceAccountKeyPath: String) {
        logger.info("Initializing Firebase Admin...")
        logger.debug("Service account file: $serviceAccountKeyPath")
        val serviceAccountKeyFile = File(serviceAccountKeyPath)
        if (!serviceAccountKeyFile.exists()) {
            error("Service account file ($serviceAccountKeyFile) does not exist")
        }
        val options = serviceAccountKeyFile.inputStream().use { input ->
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(input))
                .build()
        }
        val app = FirebaseApp.initializeApp(options)
        logger.info("Firebase Admin initialized")

        messaging = FirebaseMessaging.getInstance(app)
    }

    /**
     * Send a notification to a user.
     *
     * @return The message IDs of the notifications sent
     *
     * @throws FirebaseMessagingException If an error occurs while sending the message
     */
    suspend fun notify(notification: Notification): List<String> {
        val data = ServerDatabase { notification.serializable() }
        val email = data.userId
        val tokens = SessionsDatabase.getTokensForEmail(email)
        val type = data.type
        val payload = data.payload
        val json = serverJson.encodeToString(PushPayload.serializer(), payload)

        val messages = tokens.map { token ->
            Message.builder()
                .putData("data", json)
                .putData("type", type.name)
                .setToken(token)
                .build()
        }
        val response = messaging.sendEach(messages)
        logger.debug(
            "Sent notification to $email for ${type.name} in ${response.successCount} devices. Failed to deliver to ${response.failureCount} devices"
        )

        return response.responses.mapNotNull { it.messageId }
    }
}
