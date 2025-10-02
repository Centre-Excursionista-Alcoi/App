package org.centrexcursionistalcoi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.auth.AuthCallbackProcessor
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.AuthCallbackModel

class AuthCallbackActivity: ComponentActivity() {

    private val model by viewModels<AuthCallbackModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.dataString ?: return finish()
        val url = Url(data)
        model.processCallbackUrl(url) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

        setContent {
            AppTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
