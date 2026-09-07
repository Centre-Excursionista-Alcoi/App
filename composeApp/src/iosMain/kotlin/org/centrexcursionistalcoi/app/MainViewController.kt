package org.centrexcursionistalcoi.app

import androidx.compose.ui.window.ComposeUIViewController
import com.diamondedge.logging.FixedLogLevel
import com.diamondedge.logging.KmLogging
import com.diamondedge.logging.PrintLogger
import platform.UIKit.UIViewController

/**
 * Koin is started from Swift's `AppDelegate.application(_:didFinishLaunchingWithOptions:)` (via
 * `KoinIosKt.initKoinIos()`) rather than here, since notification setup there needs it before this ever runs.
 */
fun MainViewController(): UIViewController {
    KmLogging.setLoggers(PrintLogger(FixedLogLevel(true)))

    return ComposeUIViewController { MainApp() }
}
