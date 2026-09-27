package org.centrexcursionistalcoi.app.storage

import kotlin.io.path.Path
import org.centrexcursionistalcoi.app.ConfigProvider
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.LoggerFactory

/**
 * Configuration of the object storage for files. For Cloudflare R2:
 * - `S3_ENDPOINT`: `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`
 * - `S3_BUCKET`: the bucket name
 * - `S3_ACCESS_KEY_ID`/`S3_SECRET_ACCESS_KEY`: an R2 API token with Object Read & Write on the bucket
 * - `S3_REGION`: `auto` (the default)
 *
 * Without S3 configured, development servers store files in `FILES_PATH` (default `./files`).
 */
object FileStorageConfig : ConfigProvider() {
    val endpoint get() = getenv("S3_ENDPOINT")
    val bucket get() = getenv("S3_BUCKET")
    val accessKeyId get() = getenv("S3_ACCESS_KEY_ID")
    val secretAccessKey get() = getenv("S3_SECRET_ACCESS_KEY")
    val region get() = getenv("S3_REGION") ?: "auto"
    val filesPath get() = getenv("FILES_PATH") ?: "./files"

    private val required get() = listOf(endpoint, bucket, accessKeyId, secretAccessKey)

    fun isS3Configured() = required.all { it != null }

    fun isS3PartiallyConfigured() = required.any { it != null } && !isS3Configured()
}

object FileStorageProvider {
    private val logger = LoggerFactory.getLogger(FileStorageProvider::class.java)

    @Volatile
    private var instance: FileStorage? = null

    /**
     * The storage in use.
     * @throws IllegalStateException if [init] hasn't been called.
     */
    val current: FileStorage get() = instance ?: error("File storage has not been initialized")

    /**
     * Picks the storage from [FileStorageConfig]: S3 if configured, else a local directory in development. Refuses
     * to run without S3 outside development, since files would be lost with the container.
     */
    fun fromConfig(isDevelopment: Boolean): FileStorage {
        check(!FileStorageConfig.isS3PartiallyConfigured()) {
            "S3_ENDPOINT, S3_BUCKET, S3_ACCESS_KEY_ID and S3_SECRET_ACCESS_KEY must either all be set, or none of them."
        }
        return when {
            FileStorageConfig.isS3Configured() -> S3FileStorage.create(
                endpoint = FileStorageConfig.endpoint!!,
                bucket = FileStorageConfig.bucket!!,
                accessKeyId = FileStorageConfig.accessKeyId!!,
                secretAccessKey = FileStorageConfig.secretAccessKey!!,
                region = FileStorageConfig.region,
            )
            isDevelopment -> LocalFileStorage(Path(FileStorageConfig.filesPath)).also {
                logger.warn("S3 storage is not configured. Storing files in ${it.description}. Use only for development.")
            }
            else -> error(
                "File storage is not configured. Set S3_ENDPOINT, S3_BUCKET, S3_ACCESS_KEY_ID and S3_SECRET_ACCESS_KEY " +
                    "(Cloudflare R2). Only development servers (ENV=development) may store files locally."
            )
        }
    }

    /**
     * Initializes the storage from the configuration, and checks that it can be used.
     * @throws IllegalStateException if the storage is not configured or not available.
     */
    fun init(isDevelopment: Boolean) {
        val storage = fromConfig(isDevelopment)
        storage.checkAvailable()
        logger.info("Storing files in ${storage.description}")
        instance = storage
    }

    @VisibleForTesting
    fun useForTests(storage: FileStorage?) {
        instance = storage
    }
}
