package com.sudoplatform.sudositereputation

import android.util.LruCache
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.sudoplatform.sudositereputation.graphql.CallbackHolder
import com.sudoplatform.sudositereputation.graphql.GetSiteReputationQuery
import com.sudoplatform.sudositereputation.graphql.GetSiteReputationQuery.GetSiteReputation
import com.sudoplatform.sudositereputation.graphql.fragment.Reputation
import com.sudoplatform.sudositereputation.graphql.type.ReputationStatus
import com.sudoplatform.sudositereputation.types.SiteReputation
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.cancellation.CancellationException

/**
 * Test the correct operation of [APIClient.getSiteReputation()] using mocks.
 */
@RunWith(RobolectricTestRunner::class)
internal class APIClientTests : BaseTests() {

    private var queryHolder = CallbackHolder<GetSiteReputationQuery.Data>()

    private val mockAppSyncClient by before {
        mock<AWSAppSyncClient>().stub {
            on { query(any<GetSiteReputationQuery>()) } doReturn queryHolder.queryOperation
        }
    }

    private val spyLruCache by before {
        spy(LruCache<String, SiteReputation>(1024))
    }

    private val apiClient by before {
        APIClient(
            appSyncClient = mockAppSyncClient,
            logger = mockLogger,
            cache = spyLruCache
        )
    }

    private val query by before {
        GetSiteReputationQuery("foo.com")
    }

    @Before
    fun init() {
        queryHolder.callback = null
    }

    @After
    fun fini() {
        verifyNoMoreInteractions(mockContext, mockUserClient, mockAppSyncClient)
    }

    @Test
    fun `getSiteReputation() should resolve when no error present`() = runBlocking<Unit> {
        queryHolder.callback.shouldBe(null)

        val deferredResult = async(Dispatchers.IO) {
            apiClient.getSiteReputation("http://www.storytrain.com")
            apiClient.getSiteReputation("http://www.storytrain.com") // This should be pulled from the cache
        }
        deferredResult.start()

        delay(100L)
        queryHolder.callback.shouldNotBe(null)

        val reputation = Reputation(
            "Reputation",
            ReputationStatus.NOTMALICIOUS
        )

        val queryData = GetSiteReputationQuery.Data(
            GetSiteReputation(
                "typename",
                GetSiteReputation.Fragments(reputation)
            )
        )

        val queryResponse = com.apollographql.apollo.api.Response.builder<GetSiteReputationQuery.Data>(query)
            .data(queryData)
            .build()

        queryHolder.callback?.onResponse(queryResponse)

        deferredResult.await()

        // Service response should have been cached, make sure the service is only called once
        verify(mockAppSyncClient, times(1)).query(any<GetSiteReputationQuery>())
    }

    @Test
    fun `getSiteReputation() should throw when query response is null`() = runBlocking<Unit> {
        queryHolder.callback shouldBe null

        val nullResponse by before {
            com.apollographql.apollo.api.Response.builder<GetSiteReputationQuery.Data>(query)
                .data(null)
                .build()
        }

        val deferredResult = async(Dispatchers.IO) {
            shouldThrow<SudoSiteReputationException.FailedException> {
                apiClient.getSiteReputation("http://www.storylord.com")
            }
        }

        deferredResult.start()

        delay(100L)
        queryHolder.callback shouldNotBe null
        queryHolder.callback?.onResponse(nullResponse)

        deferredResult.await()

        verify(mockAppSyncClient).query(any<GetSiteReputationQuery>())
    }

    @Test
    fun `getSiteReputation() should throw when response has an error`() = runBlocking<Unit> {
        queryHolder.callback shouldBe null

        val errorQueryResponse by before {
            val error = com.apollographql.apollo.api.Error(
                "mock",
                emptyList(),
                mapOf("errorType" to "serviceError")
            )
            com.apollographql.apollo.api.Response.builder<GetSiteReputationQuery.Data>(query)
                .errors(listOf(error))
                .data(null)
                .build()
        }

        val deferredResult = async(Dispatchers.IO) {
            shouldThrow<SudoSiteReputationException.FailedException> {
                apiClient.getSiteReputation("http://www.storytrainanthology.com")
            }
        }
        deferredResult.start()

        delay(100L)
        queryHolder.callback shouldNotBe null
        queryHolder.callback?.onResponse(errorQueryResponse)

        verify(mockAppSyncClient).query(any<GetSiteReputationQuery>())
    }

    @Test
    fun `getSiteReputation() should not block coroutine cancellation exception`() = runBlocking<Unit> {
        mockAppSyncClient.stub {
            on { query(any<GetSiteReputationQuery>()) } doThrow CancellationException("Mock Runtime Exception")
        }

        shouldThrow<CancellationException> {
            // accessTokenService.getAccessToken("keyId", CachePolicy.CACHE_ONLY)
            apiClient.getSiteReputation("foo.com")
        }

        verify(mockAppSyncClient).query(any<GetSiteReputationQuery>())
    }
}
