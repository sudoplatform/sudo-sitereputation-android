package com.sudoplatform.sudositereputation

import android.content.Context
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.types.RealtimeReputation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

/**
 * The default implementation of [RealtimeSiteReputationClient] provided by this SDK.
 *
 * @since 2021-01-04
 */
internal class DefaultRealtimeSiteReputationClient(
    context: Context,
    private val logger: Logger,
    private val apiClient: APIClient
) : RealtimeSiteReputationClient, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    override fun close() {
        try {
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

    override suspend fun getSiteReputation(url: String): RealtimeReputation {
        return apiClient.getSiteReputation(url)
    }
}
