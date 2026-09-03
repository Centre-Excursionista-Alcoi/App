package org.centrexcursionistalcoi.app

import androidx.compose.ui.window.ComposeUIViewController
import com.diamondedge.logging.FixedLogLevel
import com.diamondedge.logging.KmLogging
import com.diamondedge.logging.PrintLogger
import org.centrexcursionistalcoi.app.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    KmLogging.setLoggers(PrintLogger(FixedLogLevel(true)))

    initKoin()

    return ComposeUIViewController { MainApp() }
}
