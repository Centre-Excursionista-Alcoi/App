package org.centrexcursionistalcoi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.auth.AuthCallbackProcessor

class AuthCallbackActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.dataString ?: return finish()
        val url = Url(data)
        runBlocking {
            AuthCallbackProcessor.processCallbackUrl(url)
        }
    }
}
