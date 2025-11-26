package org.centrexcursionistalcoi.app.platform

import com.diamondedge.logging.logging
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readRemaining
import java.awt.Desktop
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.network.getHttpClient

actual object PlatformAppUpdates {
    private val log = logging()

    private const val GITHUB_OWNER = "Centre-Excursionista-Alcoi"
    private const val GITHUB_REPO = "App"

    private val _updateAvailable = MutableStateFlow(false)
    actual val updateAvailable: Flow<Boolean> = _updateAvailable.asStateFlow()

    private val _updateProgress = MutableStateFlow<Float?>(null)
    actual val updateProgress: Flow<Float?> = _updateProgress.asStateFlow()

    private val _restartRequired = MutableStateFlow(false)
    actual val restartRequired: Flow<Boolean> = _restartRequired.asStateFlow()

    private var updateAssetUrl: String? = null
    private var downloadedFile: File? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Regex to parse SemVer (Major.Minor.Patch)
    private val versionRegex = Regex("""(\d+)\.(\d+)\.(\d+)""")

    /**
     * Call this function when the app starts.
     */
    fun checkForUpdates() {
        scope.launch {
            try {
                val client = getHttpClient()
                // Fetch latest release from GitHub
                val response = client.get("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest") {
                    headers {
                        append(HttpHeaders.Accept, "application/vnd.github.v3+json")
                    }
                }

                if (response.status == HttpStatusCode.OK) {
                    val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                    val tagName = json["tag_name"]?.jsonPrimitive?.content ?: return@launch
                    val assets = json["assets"]?.jsonArray ?: return@launch

                    val remoteVersion = tagName.removePrefix("v")

                    if (isNewerVersion(BuildKonfig.VERSION_NAME, remoteVersion)) {
                        val matchingAsset = findAssetForCurrentOs(assets, remoteVersion)
                        if (matchingAsset != null) {
                            log.d { "Update available! Current version: ${BuildKonfig.VERSION_NAME}, Remote version: $remoteVersion" }
                            updateAssetUrl = matchingAsset
                            _updateAvailable.value = true
                        } else {
                            log.d { "No suitable update asset found for current OS. Current version: ${BuildKonfig.VERSION_NAME}, Remote version: $remoteVersion" }
                        }
                    } else {
                        log.d { "No updates available. Current version: ${BuildKonfig.VERSION_NAME}, Remote version: $remoteVersion" }
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "Could not check for updates." }
            }
        }
    }

    actual fun dismissUpdateAvailable() {
        _updateAvailable.value = false
        _updateProgress.value = null
    }

    actual fun startUpdate() {
        val url = updateAssetUrl ?: return

        scope.launch {
            try {
                _updateProgress.value = 0f
                val client = getHttpClient()

                // Determine filename from URL
                val fileName = url.substringAfterLast("/")
                val targetFile = File(System.getProperty("java.io.tmpdir"), fileName)

                if (targetFile.exists()) targetFile.delete()

                // Download with progress
                client.prepareGet(url).execute { httpResponse ->
                    val channel = httpResponse.bodyAsChannel()
                    val totalBytes = httpResponse.contentLength() ?: -1L

                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(8 * 1024)
                        while (!packet.exhausted()) {
                            val bytes = packet.readByteArray()
                            targetFile.appendBytes(bytes)

                            if (totalBytes > 0) {
                                val currentProgress = targetFile.length().toFloat() / totalBytes
                                _updateProgress.value = currentProgress
                            }
                        }
                    }
                }

                downloadedFile = targetFile
                _updateProgress.value = 1f // Complete

                // Attempt to install/run immediately
                launchInstaller(targetFile)

            } catch (e: Exception) {
                log.e(e) { "Could not start update." }
                _updateProgress.value = null // Reset on failure
            }
        }
    }

    actual fun onRestartRequested() {
        // For external installers (.exe/.deb), we usually just need to close the app
        // so the installer can overwrite files (especially on Windows).
        exitProcess(0)
    }

    private fun launchInstaller(file: File) {
        try {
            val os = System.getProperty("os.name").lowercase()

            when {
                os.contains("win") -> {
                    // Run .exe
                    ProcessBuilder(file.absolutePath)
                        .start()
                    // Determine if we should flag restart required or just exit
                    _restartRequired.value = true
                }
                os.contains("nux") || os.contains("nix") -> {
                    // Try to open .deb with xdg-open or allow user to run it
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(file)
                        } else {
                            // Fallback to xdg-open
                            ProcessBuilder("xdg-open", file.absolutePath).start()
                        }
                        _restartRequired.value = true
                    } catch (e: Exception) {
                        // Fallback: Use terminal?
                        // It's hard to auto-install .deb without sudo password prompt.
                        // We assume the user environment handles the file open.
                        log.e(e) { "Could not launch installer automatically." }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Could not launch installer." }
        }
    }

    private fun findAssetForCurrentOs(assets: JsonArray, version: String): String? {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase() // Usually "amd64" or "x86_64"

        val targetName = when {
            os.contains("win") -> "CEA.App-$version.exe"
            os.contains("nux") -> {
                // Check Ubuntu version
                val releaseInfo = getLinuxReleaseInfo()
                if (releaseInfo.contains("Ubuntu") && arch.contains("64")) {
                    when {
                        releaseInfo.contains("22.04") -> "cea-app_ubuntu-22.04_amd64.deb"
                        releaseInfo.contains("24.04") -> "cea-app_ubuntu-24.04_amd64.deb"
                        else -> null // Unsupported Ubuntu version
                    }
                } else {
                    null // Not Ubuntu or not 64 bit
                }
            }
            else -> null
        }

        if (targetName == null) return null

        return assets.map { it.jsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.content == targetName }
            ?.get("browser_download_url")?.jsonPrimitive?.content
    }

    private fun getLinuxReleaseInfo(): String {
        return try {
            val file = File("/etc/os-release")
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            log.e(e) { "Could not get linux release info." }
            ""
        }
    }

    private fun isNewerVersion(current: String, remote: String): Boolean {
        val currentParts = parseVersion(current) ?: return false
        val remoteParts = parseVersion(remote) ?: return false

        for (i in 0 until 3) {
            if (remoteParts[i] > currentParts[i]) return true
            if (remoteParts[i] < currentParts[i]) return false
        }
        return false // Equal
    }

    private fun parseVersion(version: String): List<Int>? {
        val match = versionRegex.find(version) ?: return null
        return match.groupValues.drop(1).map { it.toInt() }
    }
}
