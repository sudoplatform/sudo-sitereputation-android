package com.sudoplatform.sudositereputation

import android.util.LruCache
import com.amplifyframework.api.ApiCategory
import com.amplifyframework.api.graphql.GraphQLOperation
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Consumer
import com.sudoplatform.sudositereputation.graphql.GetSiteReputationQuery
import com.sudoplatform.sudositereputation.types.SiteReputation
import com.sudoplatform.sudouser.amplify.GraphQLClient
import io.kotlintest.shouldThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.cancellation.CancellationException

/**
 * Test the correct operation of [APIClient.getSiteReputation()] using mocks.
 */
@RunWith(RobolectricTestRunner::class)
internal class APIClientTests : BaseTests() {

    private val mockGraphQLClient: ApiCategory = mock()

    private val spyLruCache by before {
        spy(LruCache<String, SiteReputation>(1024))
    }

    private val mockApiClient by before {
        APIClient(
            graphQLClient = GraphQLClient((this.mockGraphQLClient)),
            logger = mockLogger,
            cache = spyLruCache,
        )
    }

    @After
    fun fini() {
        verifyNoMoreInteractions(mockContext, mockUserClient, mockGraphQLClient)
    }

    @Test
    fun `getSiteReputation() should resolve when no error present`() = runBlocking<Unit> {
        whenever(
            mockGraphQLClient.query<String>(
                argThat {
                    this.query.equals(GetSiteReputationQuery.OPERATION_DOCUMENT)
                },
                any(),
                any(),
            ),
        ).thenAnswer {
            // this queryResponse must contain the json object you wish to represent
            // just as it comes back on the wire from the service
            val queryResponse = JSONObject(
                """
                {
                    "getSiteReputation": {
                        "__typename": 'Reputation',
                        "reputationStatus": "MALICIOUS",
                        "categories": ["fake"]
                    }
                }
                """.trimIndent(),
            )
            @Suppress("UNCHECKED_CAST")
            (it.arguments[1] as Consumer<GraphQLResponse<String>>).accept(
                GraphQLResponse(queryResponse.toString(), null),
            )
            mock<GraphQLOperation<String>>()
        }

        val deferredResult = async(Dispatchers.IO) {
            mockApiClient.getSiteReputation("http://www.storytrain.com")
            mockApiClient.getSiteReputation("http://www.storytrain.com") // This should be pulled from the cache
        }

        deferredResult.start()

        delay(100L)

        deferredResult.await()

        // Service response should have been cached, make sure the service is only called once
        verify(mockGraphQLClient, times(1)).query(any<GraphQLRequest<GetSiteReputationQuery>>(), any(), any())
    }

    @Test
    fun `getSiteReputation() should throw when response has an error`() = runBlocking<Unit> {
        whenever(
            mockGraphQLClient.query<String>(
                argThat { this.query.equals(GetSiteReputationQuery.OPERATION_DOCUMENT) },
                any(),
                any(),
            ),
        ).thenAnswer {
            // build the error response you want to deliver
            val error = GraphQLResponse.Error(
                "mock",
                emptyList(),
                emptyList(),
                mapOf("errorType" to "serviceError"),
            )
            @Suppress("UNCHECKED_CAST")
            (it.arguments[1] as Consumer<GraphQLResponse<String>>).accept(
                GraphQLResponse(null, listOf(error)),
            )
            mock<GraphQLOperation<String>>()
        }

        val deferredResult = async(Dispatchers.IO) {
            shouldThrow<SudoSiteReputationException.FailedException> {
                mockApiClient.getSiteReputation("http://www.storytrainanthology.com")
            }
        }
        deferredResult.start()

        delay(100L)

        verify(mockGraphQLClient).query(any<GraphQLRequest<GetSiteReputationQuery>>(), any(), any())
    }

    @Test
    fun `getSiteReputation() should not block coroutine cancellation exception`() = runBlocking<Unit> {
        mockGraphQLClient.stub {
            on {
                query(
                    any<GraphQLRequest<GetSiteReputationQuery>>(),
                    any(),
                    any(),
                )
            } doThrow CancellationException("Mock Runtime Exception")
        }

        shouldThrow<CancellationException> {
            mockApiClient.getSiteReputation("foo.com")
        }

        verify(mockGraphQLClient).query(any<GraphQLRequest<GetSiteReputationQuery>>(), any(), any())
    }
}
