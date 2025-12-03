package org.centrexcursionistalcoi.app.platform

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.diamondedge.logging.logging
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler

actual object PlatformAppUpdates {
    private val log = logging()

    private val _updateAvailable = MutableStateFlow(false)
    actual val updateAvailable: Flow<Boolean> get() = _updateAvailable.asStateFlow()

    private val _updateProgress = MutableStateFlow<Float?>(null)
    actual val updateProgress: Flow<Float?> get() = _updateProgress.asStateFlow()

    private val _restartRequired = MutableStateFlow(false)
    actual val restartRequired: Flow<Boolean> get() = _restartRequired.asStateFlow()

    private var appUpdateInfo: AppUpdateInfo? = null

    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                _updateProgress.value = if (totalBytesToDownload > 0) {
                    bytesDownloaded.toFloat() / totalBytesToDownload.toFloat()
                } else {
                    0f
                }
            }

            InstallStatus.DOWNLOADED -> {
                log.i { "An update has been downloaded. Prompting user to complete the update." }
                _restartRequired.value = true
            }

            InstallStatus.INSTALLING -> {
                log.i { "Update is being installed..." }
                _updateProgress.value = 1f
            }

            InstallStatus.INSTALLED -> {
                log.i { "Update installed successfully." }
                _updateAvailable.value = false
                _updateProgress.value = null
                _restartRequired.value = false
            }

            else -> {
                log.w { "Update install state: $state" }
            }
        }
    }

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var resultLauncher: ActivityResultLauncher<IntentSenderRequest>

    fun initialize(context: Context, resultLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdateManager = AppUpdateManagerFactory.create(context)
        appUpdateManager.registerListener(listener)
        this.resultLauncher = resultLauncher
    }

    fun stop() {
        appUpdateManager.unregisterListener(listener)
    }

    fun registerForActivityResult(activity: ComponentActivity) = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                log.i { "App update flow completed successfully." }
                _updateAvailable.value = false
            }

            Activity.RESULT_CANCELED -> {
                log.w { "App update flow was canceled by the user." }
                _updateAvailable.value = true
            }

            else -> {
                log.e { "App update flow failed with result code: ${result.resultCode}" }
                _updateAvailable.value = true

                GlobalAsyncErrorHandler.setError(
                    RuntimeException("App update flow failed with result code: ${result.resultCode}"),
                )
            }
        }
    }

    fun checkForUpdates(context: Context) {
        val appUpdateManager = AppUpdateManagerFactory.create(context)
        log.d { "Checking for updates..." }
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                log.i { "An update has been downloaded. Prompting user to complete the update." }
                _restartRequired.value = true
                return@addOnSuccessListener
            }

            val updateAvailability = appUpdateInfo.updateAvailability()
            if (updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                val updatePriority = appUpdateInfo.updatePriority()
                log.i { "Update available. Code: ${appUpdateInfo.availableVersionCode()}. Priority: ${updatePriority}." }
                if (updatePriority >= 4) {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        log.w { "Update has a high priority. Launching immediate update." }
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            resultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    } else {
                        log.w { "Update has a high priority. Immediate update not supported: Launching flexible update." }
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            resultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                        )
                    }
                } else if (updatePriority == 3) {
                    log.i { "Received a low-priority update. Launching flexible update." }
                    this.appUpdateInfo = appUpdateInfo
                    _updateAvailable.value = true
                } else {
                    log.d { "An update is available, but priority is low. Won't ask the user." }
                }
            } else if (updateAvailability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    resultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            } else {
                log.d { "No update available" }
            }
        }
    }

    actual fun startUpdate() {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo ?: return,
            resultLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }

    actual fun dismissUpdateAvailable() {
        log.i { "User dismissed the update available dialog." }
        _updateAvailable.value = false
    }

    actual fun onRestartRequested() {
        log.i { "User requested app restart to complete the update." }
        appUpdateManager.completeUpdate()
            .addOnSuccessListener { log.i { "Restarting app..." } }
            .addOnFailureListener { GlobalAsyncErrorHandler.setError(it) }
    }
}
