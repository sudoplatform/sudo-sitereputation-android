package com.sudoplatform.sudositereputation

import android.content.Context
import android.util.LruCache
import com.sudoplatform.sudoapiclient.ApiClientManager
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.logging.LogConstants
import com.sudoplatform.sudositereputation.storage.StorageProvider
import com.sudoplatform.sudositereputation.types.LegacySiteReputation
import com.sudoplatform.sudositereputation.types.SiteReputation
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.amplify.GraphQLClient
import java.util.Objects

/**
 * Interface encapsulating a library for interacting with the Sudo Site Reputation service.
 * @sample com.sudoplatform.sudositereputation.samples.Samples.buildClient
 *
 * @since 2021-01-04
 */
interface SudoSiteReputationClient : AutoCloseable {

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
        private var graphQLClient: GraphQLClient? = null

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
         * Provide an [GraphQLClient] for the [SiteReputationClient] to use
         * (optional input). If this is not supplied, an [GraphQLClient] will
         * be constructed and used.
         */
        fun setGraphQLClient(graphQLClient: GraphQLClient) = also {
            this.graphQLClient = graphQLClient
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
        fun build(): SudoSiteReputationClient {
            Objects.requireNonNull(context, "Context must be provided.")
            Objects.requireNonNull(sudoUserClient, "SudoUserClient must be provided.")

            val graphQLClient = graphQLClient ?: ApiClientManager.getClient(this@Builder.context!!, this@Builder.sudoUserClient!!)
            return DefaultSudoSiteReputationClient(
                context = context!!,
                logger = logger,
                apiClient = APIClient(graphQLClient = graphQLClient, logger = logger, cache = LruCache<String, SiteReputation>(1024)),
            )
        }
    }

    /**
     * Checks the host or domain of a URL to determine if it is listed as malicious.
     *
     * @param url The URL of the resource that should be checked against the reputation rulesets
     * @return [LegacySiteReputation.isMalicious] will be true if the URL should not be loaded.
     * @sample com.sudoplatform.sudositereputation.samples.Samples.getSiteReputation
     */
    @Throws(SudoSiteReputationException::class)
    suspend fun getSiteReputation(url: String): SiteReputation
}
