/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoSiteReputationClient.clearStorage] using mocks and spies.
 *
 * @since 2021-01-04
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoSiteReputationClientClearStorageTest : BaseTests() {

    @After
    fun fini() {
        verifyMocksUsedInClientInit()
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockS3Client,
            mockStorageProvider
        )
    }

    @Test
    fun `clearStorage() should call storage provider`() = runBlocking<Unit> {

        siteReputationClient.clearStorage()

        verify(mockStorageProvider).deleteFiles()
        verify(mockStorageProvider).deleteFileETags()
    }

    @Test
    fun `clearStorage() should throw when storage provider throws`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { deleteFiles() } doThrow IOException("mock")
        }

        shouldThrow<SudoSiteReputationException.FailedException> {
            siteReputationClient.clearStorage()
        }

        verify(mockStorageProvider).deleteFiles()
    }

    @Test
    fun `clearStorage() should throw when storage provider throws from eTags`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { deleteFileETags() } doThrow IOException("mock")
        }

        shouldThrow<SudoSiteReputationException.FailedException> {
            siteReputationClient.clearStorage()
        }

        verify(mockStorageProvider).deleteFiles()
        verify(mockStorageProvider).deleteFileETags()
    }

    @Test
    fun `clearStorage() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { deleteFiles() } doThrow CancellationException("mock")
        }

        shouldThrow<CancellationException> {
            siteReputationClient.clearStorage()
        }

        verify(mockStorageProvider).deleteFiles()
    }
}
