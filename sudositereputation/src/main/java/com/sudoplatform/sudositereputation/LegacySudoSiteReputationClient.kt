/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import android.content.Context
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.logging.LogConstants
import com.sudoplatform.sudositereputation.s3.readS3Configuration
import com.sudoplatform.sudositereputation.storage.DefaultStorageProvider
import com.sudoplatform.sudositereputation.storage.StorageProvider
import com.sudoplatform.sudositereputation.types.Ruleset
import com.sudoplatform.sudositereputation.types.LegacySiteReputation
import com.sudoplatform.sudouser.SudoUserClient
import java.util.Date
import java.util.Objects

/**
 * Interface encapsulating a library for interacting with the Sudo Site Reputation service.
 * @sample com.sudoplatform.sudositereputation.samples.Samples.buildClient
 *
 * @since 2021-01-04
 */
interface LegacySudoSiteReputationClient : AutoCloseable {
    val ENTITLEMENT_NAME: String

    companion object {
        /** Create a [Builder] for [LegacySudoSiteReputationClient]. */
        @JvmStatic
        fun builder() = Builder()
    }

    /**
     * Builder used to construct the [LegacySudoSiteReputationClient].
     */
    class Builder internal constructor() {
        private var context: Context? = null
        private var sudoUserClient: SudoUserClient? = null
        private var logger: Logger = Logger(LogConstants.SUDOLOG_TAG, AndroidUtilsLogDriver(LogLevel.INFO))
        private var storageProvider: StorageProvider? = null
        private var appSyncClient: AWSAppSyncClient? = null

        /**
         * Provide the application context (required input).
         */
        fun setContext(context: Context) = also {
            it.context = context
        }

        /**
         * Provide the implementation of the [SudoUserClient] used to perform
         * sign in and ownership operations (required input).
         */
        fun setSudoUserClient(sudoUserClient: SudoUserClient) = also {
            it.sudoUserClient = sudoUserClient
        }

        /**
         * Provide the implementation of the [StorageProvider] used to read and write cached
         * metadata and contents and the allow list (optional input). If a value is not supplied
         * a default implementation will be used.
         */
        fun setStorageProvider(storageProvider: StorageProvider) = also {
            it.storageProvider = storageProvider
        }

        /**
         * Provide the implementation of the [Logger] used for logging errors (optional input).
         * If a value is not supplied a default implementation will be used.
         */
        fun setLogger(logger: Logger) = also {
            it.logger = logger
        }

        /**
         * Construct the [LegacySudoSiteReputationClient]. Will throw a [NullPointerException] if
         * the [context] or [sudoUserClient] have not been provided or [ConfigurationException]
         * if the sudoplatformconfig.json file is missing the region or bucket item in the
         * identityService stanza.
         */
        @Throws(NullPointerException::class, SudoSiteReputationException.ConfigurationException::class)
        fun build(): LegacySudoSiteReputationClient {
            Objects.requireNonNull(context, "Context must be provided.")
            Objects.requireNonNull(sudoUserClient, "SudoUserClient must be provided.")

            val (region, bucket) = readS3Configuration(context!!, logger)

            return DefaultLegacySiteReputationClient(
                context = context!!,
                sudoUserClient = sudoUserClient!!,
                logger = logger,
                region = region,
                bucket = bucket,
                storageProvider = storageProvider ?: DefaultStorageProvider(context!!)
            )
        }
    }

    /**
     * Request the [Ruleset]s are updated from the service.
     *
     * @sample com.sudoplatform.sudositereputation.samples.Samples.update
     */
    @Throws(SudoSiteReputationException::class)
    suspend fun update()

    /**
     * The date/time of when the last call to [update] was made, null if it has never been
     * called or if the cached rulesets have been deleted.
     *
     * @sample com.sudoplatform.sudositereputation.samples.Samples.lastUpdatePerformedAt
     */
    val lastUpdatePerformedAt: Date?

    /**
     * Checks the host or domain of a URL to determine if it is listed as malicious.
     *
     * @param url The URL of the resource that should be checked against the reputation rulesets
     * @return [LegacySiteReputation.isMalicious] will be true if the URL should not be loaded.
     * @sample com.sudoplatform.sudositereputation.samples.Samples.getSiteReputation
     */
    @Throws(SudoSiteReputationException::class)
    suspend fun getSiteReputation(url: String): LegacySiteReputation

    /**
     * Delete all cached data.
     *
     * @sample com.sudoplatform.sudositereputation.samples.Samples.clearStorage
     */
    @Throws(SudoSiteReputationException::class)
    suspend fun clearStorage()
}
