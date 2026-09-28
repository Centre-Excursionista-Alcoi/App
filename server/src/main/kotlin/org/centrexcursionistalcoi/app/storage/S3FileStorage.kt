package org.centrexcursionistalcoi.app.storage

import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.fileSize
import java.time.Duration
import org.jetbrains.annotations.VisibleForTesting
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception

/**
 * Stores objects in an S3-compatible bucket: Cloudflare R2 in production.
 */
class S3FileStorage(
    private val bucket: String,
    private val client: S3Client,
    endpoint: String? = null,
) : FileStorage {
    companion object {
        /**
         * Creates a client for the given S3-compatible [endpoint].
         *
         * For Cloudflare R2, [endpoint] is `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`, and [region] is `auto`.
         */
        fun create(
            endpoint: String,
            bucket: String,
            accessKeyId: String,
            secretAccessKey: String,
            region: String = "auto",
        ): S3FileStorage {
            val client = S3Client.builder()
                .endpointOverride(URI(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .httpClient(
                    UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(10))
                        .socketTimeout(Duration.ofSeconds(60))
                        .build()
                )
                // R2 and MinIO both support path-style addressing, which doesn't need a DNS name per bucket
                .forcePathStyle(true)
                // R2 doesn't support the checksums the SDK adds by default since 2.30, send them only when required
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
                .build()
            return S3FileStorage(bucket, client, endpoint)
        }

        private fun S3Exception.isNotFound() = this is NoSuchKeyException || statusCode() == 404
    }

    override val description: String = "s3://$bucket" + (endpoint?.let { " at $it" } ?: "")

    override fun put(key: String, bytes: ByteArray, contentType: String) {
        client.putObject(
            { it.bucket(bucket).key(key).contentType(contentType).contentLength(bytes.size.toLong()) },
            RequestBody.fromBytes(bytes),
        )
    }

    override fun put(key: String, input: InputStream, size: Long, contentType: String) {
        client.putObject(
            { it.bucket(bucket).key(key).contentType(contentType).contentLength(size) },
            RequestBody.fromInputStream(input, size),
        )
    }

    override fun put(key: String, file: Path, contentType: String) {
        // Unlike a stream, a file can be read again if the request has to be retried
        client.putObject(
            { it.bucket(bucket).key(key).contentType(contentType).contentLength(file.fileSize()) },
            RequestBody.fromFile(file),
        )
    }

    override fun open(key: String): InputStream = try {
        client.getObject { it.bucket(bucket).key(key) }
    } catch (e: S3Exception) {
        if (e.isNotFound()) throw StoredObjectNotFoundException(key, e) else throw e
    }

    override fun head(key: String): StoredObjectInfo? = try {
        StoredObjectInfo(size = client.headObject { it.bucket(bucket).key(key) }.contentLength())
    } catch (e: S3Exception) {
        if (e.isNotFound()) null else throw e
    }

    override fun delete(key: String) {
        // S3 answers a successful delete also when there's no object
        client.deleteObject { it.bucket(bucket).key(key) }
    }

    override fun keys(prefix: String): List<String> =
        client.listObjectsV2Paginator { it.bucket(bucket).prefix(prefix) }
            .contents()
            .map { it.key() }

    override fun checkAvailable() {
        try {
            client.headBucket { it.bucket(bucket) }
        } catch (e: S3Exception) {
            throw IllegalStateException("Bucket $bucket is not accessible (HTTP ${e.statusCode()}): ${e.message}", e)
        }
    }

    override fun close() {
        client.close()
    }

    @VisibleForTesting
    internal fun createBucket() {
        client.createBucket { it.bucket(bucket) }
    }
}
