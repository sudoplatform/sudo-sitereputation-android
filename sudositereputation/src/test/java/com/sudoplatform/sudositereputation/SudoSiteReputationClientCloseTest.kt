/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoSiteReputationClient.close] using mocks and spies.
 *
 * @since 2021-01-05
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoSiteReputationClientCloseTest : BaseTests() {

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
    fun `close() should call reputation provider`() = runBlocking<Unit> {

        siteReputationClient.close()

        verify(mockReputationProvider).close()
    }

    @Test
    fun `close() should suppress when reputation provider throws`() = runBlocking<Unit> {

        mockReputationProvider.stub {
            on { close() } doThrow IOException("mock")
        }

        siteReputationClient.close()

        verify(mockReputationProvider).close()
    }

    @Test
    fun `close() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockReputationProvider.stub {
            on { close() } doThrow CancellationException("mock")
        }

        shouldThrow<CancellationException> {
            siteReputationClient.close()
        }

        verify(mockReputationProvider).close()
    }
}
