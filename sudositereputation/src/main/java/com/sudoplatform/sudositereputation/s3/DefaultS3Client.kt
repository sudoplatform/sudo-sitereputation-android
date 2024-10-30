/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation.s3

import android.content.Context
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudouser.IdGenerator
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Date
import kotlin.coroutines.resume

/**
 * Default S3 client implementation.
 *
 * @param context Android app context.
 * @param sudoUserClient [com.sudoplatform.sudouser.SudoUserClient] used for authenticating to AWS S3.
 * @param region The AWS region in which the bucket resides
 * @param bucket The identifier of the S3 bucket
 * @param timeoutMs The timeout in milliseconds of S3 connections
 * @param logger The Sudo logger for complaints
 * @param idGenerator Generator of unique identifiers for files and S3 objects
 */
internal class DefaultS3Client(
    context: Context,
    private val sudoUserClient: SudoUserClient,
    override val region: String,
    override val bucket: String,
    private val logger: Logger,
    private val timeoutMs: Int = DEFAULT_TIMEOUT,
    private val idGenerator: IdGenerator = DefaultIdGenerator(),
) : S3Client {

    companion object {
        internal const val DEFAULT_TIMEOUT = 10_000

        /**
         * Allow a margin of time when checking if the credentials must be refreshed
         * so there is enough time for the current operation to be completed before
         * the token expires.
         */
        internal const val REFRESH_CREDENTIALS_MARGIN_MS = 10_000
        private val EPOCH = Date(0L)
    }

    private val transferUtility: TransferUtility

    private val amazonS3Client: AmazonS3Client

    private val credentialsProvider: CognitoCredentialsProvider = sudoUserClient.getCredentialsProvider()

    init {
        val s3ClientConfig = ClientConfiguration().apply {
            connectionTimeout = timeoutMs
            socketTimeout = timeoutMs
        }
        amazonS3Client = AmazonS3Client(credentialsProvider, Region.getRegion(region), s3ClientConfig)
        transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(amazonS3Client)
            .defaultBucket(bucket)
            .build()
    }

    override suspend fun download(key: String): ByteArray {
        logger.info("Downloading a blob from S3.")
        refreshCredentials()

        return suspendCancellableCoroutine { cont ->
            val id = idGenerator.generateId()
            val tmpFile = File.createTempFile(id, ".tmp")
            val observer = transferUtility.download(bucket, key, tmpFile)
            observer.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    if (TransferState.COMPLETED == state) {
                        logger.info("S3 download completed successfully.")
                        if (cont.isActive) {
                            cont.resume(tmpFile.readBytes())
                        }
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    logger.debug("S3 download progress changed: id=$id, bytesCurrent=$bytesCurrent, bytesTotal=$bytesTotal")
                }

                override fun onError(id: Int, e: Exception?) {
                    throw S3Exception.DownloadException(e?.message, cause = e)
                }
            })
        }
    }

    override suspend fun list(path: String, limit: Int): List<S3Client.S3ObjectInfo> {
        logger.info("Listing files from S3.")
        refreshCredentials()

        val listRequest = ListObjectsV2Request().apply {
            prefix = path
            maxKeys = limit
            bucketName = bucket
        }
        val listResponse = amazonS3Client.listObjectsV2(listRequest)

        return listResponse.objectSummaries.map { objectSummary ->
            val metadata = amazonS3Client.getObjectMetadata(bucket, objectSummary.key)
                ?: throw S3Exception.MetadataException("Missing S3 object metadata")
            if (metadata.userMetadata == null || metadata.userMetadata.isEmpty()) {
                throw S3Exception.MetadataException("Empty S3 object user metadata")
            }
            S3Client.S3ObjectInfo(
                key = objectSummary.key,
                eTag = metadata.eTag,
                lastModified = metadata.lastModified,
                userMetadata = metadata.userMetadata,
            )
        }
    }

    /**
     * If the access token will expire soon then perform a refresh. Also refresh the
     * credentials used by the S3 client.
     */
    private suspend fun refreshCredentials() {
        val aFewSecondsFromNow = Date(System.currentTimeMillis() + REFRESH_CREDENTIALS_MARGIN_MS)
        val tokenExpiry = sudoUserClient.getTokenExpiry() ?: EPOCH
        if (tokenExpiry.before(aFewSecondsFromNow)) {
            sudoUserClient.getRefreshToken()?.let { refreshToken ->
                sudoUserClient.refreshTokens(refreshToken)
            }
            credentialsProvider.refresh()
        }
    }
}
