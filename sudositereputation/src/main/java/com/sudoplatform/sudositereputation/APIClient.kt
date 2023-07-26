package com.sudoplatform.sudositereputation

import android.util.LruCache
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.exception.ApolloException
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.appsync.enqueue
import com.sudoplatform.sudositereputation.graphql.GetSiteReputationQuery
import com.sudoplatform.sudositereputation.transformers.SudoSiteReputationExceptionTransformer
import com.sudoplatform.sudositereputation.transformers.SudoSiteReputationTransformer
import com.sudoplatform.sudositereputation.types.SiteReputation

internal class APIClient(
    private val appSyncClient: AWSAppSyncClient,
    private val logger: Logger,
    private val cache: LruCache<String, SiteReputation>?
) {

    fun clearCache() {
        cache?.evictAll()
    }

    companion object {
        /** Exception messages */
        private const val MISSING_RESPONSE = "Received empty response"

        /** Errors returned from the service */
        private const val ERROR_TYPE = "errorType"
        private const val ERROR_SERVICE = "ServiceError"
    }

    suspend fun getSiteReputation(uri: String): SiteReputation {
        if (cache?.get(uri) != null) {
            // if found in cache, return the item
            return cache.get(uri)
        } else {
            try {
                val query = GetSiteReputationQuery.builder()
                    .uri(uri)
                    .build()

                val response = appSyncClient.query(query)
                    .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                    .enqueue()

                if (response.hasErrors()) {
                    logger.warning("Unexpected query response. ${response.errors()}")
                    throw interpretError(response.errors().first())
                }

                response.data()?.siteReputation?.fragments()?.reputation()?.let {
                    // Cache the response here
                    val reputation = SudoSiteReputationTransformer.toReputationFromGraphQL(it)
                    cache?.put(uri, reputation)

                    return reputation
                }
                throw SudoSiteReputationException.FailedException(MISSING_RESPONSE)
            } catch (e: Throwable) {
                logger.debug("unexpected error $e")
                when (e) {
                    is ApolloException -> throw SudoSiteReputationException.FailedException(cause = e)
                    else -> throw SudoSiteReputationExceptionTransformer.interpretException(e)
                }
            }
        }
    }

    fun interpretError(e: Error): SudoSiteReputationException {
        val error = e.customAttributes()[ERROR_TYPE]?.toString() ?: ""
        if (error.contains(ERROR_SERVICE)) {
            // At time of writing the service only returns "ServiceError".
            return SudoSiteReputationException.FailedException(e.toString())
        }
        return SudoSiteReputationException.FailedException(e.toString())
    }
}
