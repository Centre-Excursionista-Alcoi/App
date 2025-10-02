package org.centrexcursionistalcoi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.auth.AuthCallbackProcessor
import org.centrexcursionistalcoi.app.viewmodel.AuthCallbackModel

class AuthCallbackActivity: ComponentActivity() {

    private val model by viewModels<AuthCallbackModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.dataString ?: return finish()
        val url = Url(data)
        model.processCallbackUrl(url)
    }
}
