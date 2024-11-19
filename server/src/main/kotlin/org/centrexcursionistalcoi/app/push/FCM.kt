package org.centrexcursionistalcoi.app.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.File

object FCM {
    fun initialize(serviceAccountKeyPath: String) {
        val options = File(serviceAccountKeyPath).inputStream().use { input ->
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(input))
                .build()
        }
        FirebaseApp.initializeApp(options)
    }
}
