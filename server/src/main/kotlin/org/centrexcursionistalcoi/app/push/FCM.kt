package org.centrexcursionistalcoi.app.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import java.io.File
import kotlinx.serialization.KSerializer
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
     * @param userId The ID of the user to send the notification to
     * @param type The type of notification
     * @param data The data to send
     * @param serializer The serializer to use for the data
     *
     * @return The message ID
     *
     * @throws FirebaseMessagingException If an error occurs while sending the message
     */
    fun <DataType> notify(userId: String, type: NotificationType, data: DataType, serializer: KSerializer<DataType>): String {
        val topic = PushTopic.topic(userId)
        val json = serverJson.encodeToString(serializer, data)

        val message = Message.builder()
            .putData("data", json)
            .putData("type", type.name)
            .setTopic(topic)
            .build()
        return messaging.send(message)
            .also { logger.debug("Sent notification to $userId for ${type.name}: $it") }
    }
}
