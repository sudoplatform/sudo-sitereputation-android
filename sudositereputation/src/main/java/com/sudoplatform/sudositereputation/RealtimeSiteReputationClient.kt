package com.sudoplatform.sudositereputation

import android.content.Context
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.sudoplatform.sudoapiclient.ApiClientManager
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.logging.LogConstants
import com.sudoplatform.sudositereputation.storage.StorageProvider
import com.sudoplatform.sudositereputation.types.RealtimeReputation
import com.sudoplatform.sudositereputation.types.SiteReputation
import com.sudoplatform.sudouser.SudoUserClient
import java.util.Objects

/**
 * Interface encapsulating a library for interacting with the Sudo Site Reputation service.
 * @sample com.sudoplatform.sudositereputation.samples.Samples.buildClient
 *
 * @since 2021-01-04
 */
interface RealtimeSiteReputationClient : AutoCloseable {

    companion object {
        /** Create a [Builder] for [SudoSiteReputationClient]. */
        @JvmStatic
        fun builder() = Builder()
    }

    /**
     * Builder used to construct the [SudoSiteReputationClient].
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
         * Provide an [AWSAppSyncClient] for the [RealtimeReputationClient] to use
         * (optional input). If this is not supplied, an [AWSAppSyncClient] will
         * be constructed and used.
         */
        fun setAppSyncClient(appSyncClient: AWSAppSyncClient) = also {
            this.appSyncClient = appSyncClient
        }

        /**
         * Provide the implementation of the [Logger] used for logging errors (optional input).
         * If a value is not supplied a default implementation will be used.
         */
        fun setLogger(logger: Logger) = also {
            it.logger = logger
        }

        /**
         * Construct the [SudoSiteReputationClient]. Will throw a [NullPointerException] if
         * the [context] or [sudoUserClient] have not been provided or [ConfigurationException]
         * if the sudoplatformconfig.json file is missing the region or bucket item in the
         * identityService stanza.
         */
        @Throws(NullPointerException::class, SudoSiteReputationException.ConfigurationException::class)
        fun build(): RealtimeSiteReputationClient {
            Objects.requireNonNull(context, "Context must be provided.")
            Objects.requireNonNull(sudoUserClient, "SudoUserClient must be provided.")

            val appSyncClient = appSyncClient ?: ApiClientManager.getClient(this@Builder.context!!, this@Builder.sudoUserClient!!)
            return DefaultRealtimeSiteReputationClient(
                context = context!!,
                logger = logger,
                apiClient = APIClient(appSyncClient = appSyncClient, logger = logger)
            )
        }
    }

    /**
     * Checks the host or domain of a URL to determine if it is listed as malicious.
     *
     * @param url The URL of the resource that should be checked against the reputation rulesets
     * @return [SiteReputation.isMalicious] will be true if the URL should not be loaded.
     * @sample com.sudoplatform.sudositereputation.samples.Samples.getSiteReputation
     */
    @Throws(SudoSiteReputationException::class)
    suspend fun getSiteReputation(url: String): RealtimeReputation
}
