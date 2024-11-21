package org.centrexcursionistalcoi.app.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import java.io.File
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.database.SessionsDatabase
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
     * It will include [data] serialized as Json using [serializer] in the `data` field.
     *
     * @param email The email of the user to send the notification to
     * @param type The type of notification
     * @param data The data to send
     * @param serializer The serializer to use for the data
     *
     * @return The message IDs of the notifications sent
     *
     * @throws FirebaseMessagingException If an error occurs while sending the message
     */
    suspend fun <DataType> notify(
        email: String,
        type: NotificationType,
        data: DataType,
        serializer: KSerializer<DataType>
    ): List<String> {
        val tokens = SessionsDatabase.getTokensForEmail(email)
        val json = serverJson.encodeToString(serializer, data)

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
