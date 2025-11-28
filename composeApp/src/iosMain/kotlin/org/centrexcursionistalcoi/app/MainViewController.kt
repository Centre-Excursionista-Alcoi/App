package org.centrexcursionistalcoi.app

import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.NavController
import com.diamondedge.logging.FixedLogLevel
import com.diamondedge.logging.KmLogging
import com.diamondedge.logging.PrintLogger
import kotlinx.coroutines.runBlocking
import org.centrexcursionistalcoi.app.storage.DriverFactory
import org.centrexcursionistalcoi.app.storage.createDatabase
import org.centrexcursionistalcoi.app.storage.databaseInstance
import platform.UIKit.UIViewController

lateinit var navController: NavController
    private set

fun MainViewController(): UIViewController {
    KmLogging.setLoggers(PrintLogger(FixedLogLevel(true)))

    // Initialize the database
    databaseInstance = runBlocking { createDatabase(DriverFactory()) }

    return ComposeUIViewController { App { navController = it } }
}
