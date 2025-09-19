package org.centrexcursionistalcoi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.datlag.sekret.NativeLoader
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.auth.tokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.AndroidDataStoreSettingsStore
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore

@OptIn(ExperimentalOpenIdConnect::class)
class MainActivity : ComponentActivity() {
    // There should only be one instance of this factory.
    // The flow should also be created and started from an
    // Application or ViewModel scope, so it persists Activity.onDestroy() e.g. on low memory
    // and is still able to process redirect results during login.
    val codeAuthFlowFactory = AndroidCodeAuthFlowFactory(useWebView = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Napier.base(DebugAntilog())

        secretsBinaryLoaded = NativeLoader.loadLibrary("sekret")

        codeAuthFlowFactory.registerActivity(this)

        tokenStore = SettingsTokenStore(settings = AndroidDataStoreSettingsStore(this))

        setContent {
            MainApp(codeAuthFlowFactory)
        }
    }
}
