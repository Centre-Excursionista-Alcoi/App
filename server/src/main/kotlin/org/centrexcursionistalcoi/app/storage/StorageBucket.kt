package org.centrexcursionistalcoi.app.storage

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.smithy.kotlin.runtime.net.url.Url
import java.io.Closeable
import org.slf4j.LoggerFactory

object StorageBucket : Closeable {
    private val logger = LoggerFactory.getLogger(StorageBucket::class.java)

    private const val BUCKET_NAME = "cea-app"

    private var s3: S3Client? = null

    suspend fun init() {
        val r2AccountId = System.getenv("R2_ACCOUNT_ID") ?: error("R2_ACCOUNT_ID not set")
        val r2AccessKeyId = System.getenv("R2_ACCESS_KEY_ID") ?: error("R2_ACCESS_KEY_ID not set")
        val r2AccessKeySecret = System.getenv("R2_ACCESS_KEY_SECRET") ?: error("R2_ACCESS_KEY_SECRET not set")

        logger.info("Initializing R2 storage bucket...")
        val s3 = S3Client {
            region = "auto"
            endpointUrl = Url.parse("https://$r2AccountId.r2.cloudflarestorage.com")
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = r2AccessKeyId
                secretAccessKey = r2AccessKeySecret
            }
        }.also { s3 = it }

        logger.info("Verifying R2 connection and checking that bucket exists...")

        logger.debug("Listing R2 Buckets...")
        val response = s3.listBuckets(ListBucketsRequest {})
        val bucket = response.buckets?.find { bucket -> bucket.name == BUCKET_NAME }
        if (bucket != null) {
            logger.info("R2 Bucket found.")
        } else {
            error("R2 Bucket not found!")
        }
    }

    override fun close() {
        s3?.close()
        s3 = null
    }
}
