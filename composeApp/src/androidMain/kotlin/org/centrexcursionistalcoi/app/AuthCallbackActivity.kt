package org.centrexcursionistalcoi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cea_app.composeapp.generated.resources.*
import io.ktor.http.Url
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.ui.screen.LoadingScreen
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.ui.utils.unknown
import org.centrexcursionistalcoi.app.viewmodel.AuthCallbackModel
import org.jetbrains.compose.resources.stringResource

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
            val error by model.error.collectAsState()

            AppTheme {
                LoadingScreen(
                    error = error,
                    progress = null,
                    errorTitle = stringResource(Res.string.auth_callback_error_title),
                    errorMessageConverter = { error ->
                        if (error is ServerException) {
                            if (error.errorCode != null) {
                                val errorMessage = error.toError()?.description ?: error.message
                                stringResource(Res.string.auth_callback_error_server_code, error.errorCode ?: 0, errorMessage ?: unknown())
                            } else {
                                stringResource(Res.string.auth_callback_error_server_http, error.responseStatusCode ?: -1, error.responseBody ?: "N/A")
                            }
                        } else {
                            error.message ?: stringResource(Res.string.error_unknown, error::class.simpleName!!)
                        }
                    }
                )
            }
        }
    }
}
