/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.sudoplatform.sudositereputation.DefaultSiteReputationClient.Companion.LAST_UPDATED_FILE
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoSiteReputationClient.getSiteReputation] using mocks and spies.
 *
 * @since 2021-01-05
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoSiteReputationClientGetSiteReputationTest : BaseTests() {

    @After
    fun fini() {
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

        verify(mockStorageProvider).read(LAST_UPDATED_FILE)
        verify(mockStorageProvider).read(argWhere { it != LAST_UPDATED_FILE })
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

        verify(mockStorageProvider).read(LAST_UPDATED_FILE)
        verify(mockStorageProvider).read(argWhere { it != LAST_UPDATED_FILE })
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

        verify(mockStorageProvider).read(LAST_UPDATED_FILE)
        verify(mockStorageProvider).read(argWhere { it != LAST_UPDATED_FILE })
        verify(mockReputationProvider).checkIsUrlMalicious(anyString())
    }
}
