/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import com.sudoplatform.sudositereputation.DefaultLegacySiteReputationClient.Companion.LAST_UPDATED_FILE
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [LegacySudoSiteReputationClient.getSiteReputation] using mocks and spies.
 *
 * @since 2021-01-05
 */
@RunWith(RobolectricTestRunner::class)
internal class LegacySudoSiteReputationClientGetSiteReputationTest : BaseTests() {

    @After
    fun finish() {
        verifyMocksUsedInClientInit()
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockS3Client,
            mockStorageProvider,
            mockReputationProvider
        )
    }

    @Test
    fun `checkIsUrlMalicious() should call reputation provider`() = runBlocking<Unit> {
        siteReputationClient.getSiteReputation("a")

        verify(mockReputationProvider).close()
        verify(mockStorageProvider).read(LAST_UPDATED_FILE)
        verify(mockStorageProvider, times(3)).read(argWhere { it != LAST_UPDATED_FILE })
        verify(mockReputationProvider).checkIsUrlMalicious(anyString())
    }

    @Test
    fun `checkIsUrlMalicious() should throw when ruleset not downloaded`() = runBlocking<Unit> {
        mockStorageProvider.stub {
            on { read(LAST_UPDATED_FILE) } doReturn null
        }

        shouldThrow<SudoSiteReputationException.RulesetNotFoundException> {
            siteReputationClient.getSiteReputation("a")
        }

        verify(mockReputationProvider).close()
        verify(mockStorageProvider).read(LAST_UPDATED_FILE)
    }

    @Test
    fun `checkIsUrlMalicious() should throw when reputation provider throws`() = runBlocking<Unit> {
        mockReputationProvider.stub {
            onBlocking { checkIsUrlMalicious(anyString()) } doThrow SudoSiteReputationException.DataFormatException("mock")
        }

        shouldThrow<SudoSiteReputationException.DataFormatException> {
            siteReputationClient.getSiteReputation("a")
        }

        verify(mockReputationProvider).close()
        verify(mockStorageProvider).read(LAST_UPDATED_FILE)
        verify(mockStorageProvider, times(3)).read(argWhere { it != LAST_UPDATED_FILE })
        verify(mockReputationProvider).checkIsUrlMalicious(anyString())
    }

    @Test
    fun `checkIsUrlMalicious() should not block coroutine cancellation exception`() = runBlocking<Unit> {
        mockReputationProvider.stub {
            onBlocking { checkIsUrlMalicious(anyString()) } doThrow CancellationException("mock")
        }

        shouldThrow<CancellationException> {
            siteReputationClient.getSiteReputation("a")
        }

        verify(mockReputationProvider).close()
        verify(mockStorageProvider).read(LAST_UPDATED_FILE)
        verify(mockStorageProvider, times(3)).read(argWhere { it != LAST_UPDATED_FILE })
        verify(mockReputationProvider).checkIsUrlMalicious(anyString())
    }

    @Test
    fun `ENTITLEMENT_NAME should not be null and should have the correct value`() = runBlocking {
        val entitlementName: String = siteReputationClient.ENTITLEMENT_NAME
        entitlementName shouldNotBe null
        entitlementName shouldBe "sudoplatform.sr.srUserEntitled"

        verify(mockReputationProvider).close()
    }
}
