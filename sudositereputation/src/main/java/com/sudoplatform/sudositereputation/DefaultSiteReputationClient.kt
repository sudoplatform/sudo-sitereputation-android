/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.reputation.DefaultReputationProvider
import com.sudoplatform.sudositereputation.reputation.ReputationProvider
import com.sudoplatform.sudositereputation.s3.DefaultS3Client
import com.sudoplatform.sudositereputation.s3.S3Client
import com.sudoplatform.sudositereputation.s3.S3Exception
import com.sudoplatform.sudositereputation.storage.StorageProvider
import com.sudoplatform.sudositereputation.transformers.RulesetTransformer
import com.sudoplatform.sudositereputation.types.Ruleset
import com.sudoplatform.sudositereputation.types.SiteReputation
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Date
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

/**
 * The default implementation of [SudoSiteReputationClient] provided by this SDK.
 *
 * @since 2021-01-04
 */
internal class DefaultSiteReputationClient(
    context: Context,
    private val logger: Logger,
    sudoUserClient: SudoUserClient,
    private val region: String,
    private val bucket: String,
    private val storageProvider: StorageProvider,
    @VisibleForTesting
    private val s3Client: S3Client = DefaultS3Client(context, sudoUserClient, region, bucket, logger),
    @VisibleForTesting
    private val reputationProvider: ReputationProvider = DefaultReputationProvider(logger),
    override val ENTITLEMENT_NAME: String = "sudoplatform.sr.srUserEntitled"
) : SudoSiteReputationClient, CoroutineScope {

    companion object {
        /** Reputation Ruleset file names and paths */
        @VisibleForTesting
        internal const val MALICIOUS_DOMAINS_FILE = "urlhaus-filter-domains-online.txt"

        @VisibleForTesting
        internal const val MALICIOUS_DOMAINS_SUBPATH = "MALICIOUSDOMAIN"

        @VisibleForTesting
        internal const val S3_TOP_PATH = "/reputation-lists"

        @VisibleForTesting
        internal const val LAST_UPDATED_FILE = "last_update_performed_at.txt"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    /**
     * The date/time of when the last call to [update] was made, null if it has never been
     * called or if the cached rulesets have been deleted.
     */
    override val lastUpdatePerformedAt: Date?
        get() = calculateLastUpdatePerformedAt()

    init {
        launch {
            setupReputationProvider()
        }
    }

    override suspend fun clearStorage() {
        close()
        try {
            storageProvider.deleteFiles()
            storageProvider.deleteFileETags()
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override fun close() {
        try {
            reputationProvider.close()
            coroutineContext.cancelChildren()
            coroutineContext.cancel()
        } catch (e: CancellationException) {
            // Never suppress this exception it's used by coroutines to cancel outstanding work
            throw e
        } catch (e: Throwable) {
            // Suppress and log anything bad that happened while closing
            logger.warning("Error while closing $e")
        }
    }

    @VisibleForTesting
    internal suspend fun listRulesets(): List<Ruleset> {
        try {
            return RulesetTransformer.toRulesetList(
                s3Client.list(path = S3_TOP_PATH)
            )
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override suspend fun update() {
        try {
            var isReputationProviderSetupRequired = false
            listRulesets().forEach { ruleset ->
                val (subPath, fileName) = ruleset.type.toPathAndFileName()
                    ?: run {
                        logger.debug("Unsupported ruleset ${ruleset.type} requested")
                        return@forEach
                    }
                val localETag = storageProvider.readFileETag(fileName)
                if (ruleset.eTag != localETag) {
                    // eTag from the service is different to what we have locally, this
                    // means the rules have been updated on the backend
                    val s3Path = makeS3Path(subPath, fileName)
                    s3Client.download(s3Path).also { rulesetBytes ->
                        storageProvider.write(fileName, rulesetBytes)
                        storageProvider.writeFileETag(fileName, ruleset.eTag)
                    }
                    isReputationProviderSetupRequired = true
                }
            }
            if (isReputationProviderSetupRequired) {
                setupReputationProvider()
            }
            val timestamp = System.currentTimeMillis().toString(10).toByteArray()
            storageProvider.write(LAST_UPDATED_FILE, timestamp)
        } catch (e: S3Exception.DownloadException) {
            logger.debug("Reputation data not found $e")
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    private fun Ruleset.Type.toPathAndFileName(): Pair<String, String>? {
        return when (this) {
            Ruleset.Type.MALICIOUS_DOMAINS -> Pair(MALICIOUS_DOMAINS_SUBPATH, MALICIOUS_DOMAINS_FILE)
            else -> null
        }
    }

    private fun makeS3Path(path: String, fileName: String): String {
        return "$S3_TOP_PATH/$path/$fileName"
    }

    override suspend fun getSiteReputation(url: String): SiteReputation {
        lastUpdatePerformedAt
            ?: throw SudoSiteReputationException.RulesetNotFoundException(
                "Reputation data is not present. Please call update to obtain the latest reputation data."
            )
        try {
            return SiteReputation(reputationProvider.checkIsUrlMalicious(url))
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    private suspend fun setupReputationProvider() {
        try {
            logger.info("Starting reputation initialization.")
            val rules = getRules(Ruleset.Type.MALICIOUS_DOMAINS)
            if (rules != null) {
                reputationProvider.close()
                reputationProvider.setRules(rules)
                logger.info("Reputation initialization completed successfully.")
            } else {
                logger.info("Reputation initialization skipped, rules have not been downloaded.")
            }
        } catch (e: CancellationException) {
            // Never suppress this exception it's used by coroutines to cancel outstanding work
            throw e
        } catch (e: Throwable) {
            logger.outputError(Error(e))
            logger.error("Reputation initialization failed $e")
        }
    }

    private fun getRules(rulesetType: Ruleset.Type): ByteArray? {
        val (_, fileName) = rulesetType.toPathAndFileName()
            ?: run {
                logger.debug("Unsupported ruleset $rulesetType requested")
                return null
            }
        return storageProvider.read(fileName)
    }

    private fun calculateLastUpdatePerformedAt(): Date? {
        try {
            val timestampBytes = storageProvider.read(LAST_UPDATED_FILE)
                ?: return null
            val timestamp = String(timestampBytes, Charsets.UTF_8)
            return Date(timestamp.toLong())
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    /**
     * Interpret an exception from SudoUserClient or S3 and map it to an exception
     * declared in this SDK's API that the caller is expecting.
     *
     * @param exception The exception from the secure value client.
     * @return The exception mapped to [SudoSiteReputationException]
     * or [CancellationException]
     */
    private fun interpretException(exception: Throwable): Throwable {
        return when (exception) {
            is CancellationException, // Never wrap or reinterpret Kotlin coroutines cancellation exception
            is SudoSiteReputationException -> exception
            is S3Exception.MetadataException -> SudoSiteReputationException.DataFormatException(cause = exception)
            is S3Exception -> throw SudoSiteReputationException.FailedException(cause = exception)
            is NotAuthorizedException -> throw SudoSiteReputationException.UnauthorizedUserException(cause = exception)
            is IOException -> throw SudoSiteReputationException.FailedException(cause = exception)
            else -> SudoSiteReputationException.UnknownException(exception)
        }
    }
}
