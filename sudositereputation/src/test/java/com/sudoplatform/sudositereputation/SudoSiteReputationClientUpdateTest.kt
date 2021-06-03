/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudositereputation.DefaultSiteReputationClient.Companion.LAST_UPDATED_FILE
import com.sudoplatform.sudositereputation.DefaultSiteReputationClient.Companion.MALICIOUS_DOMAINS_FILE
import com.sudoplatform.sudositereputation.DefaultSiteReputationClient.Companion.S3_TOP_PATH
import com.sudoplatform.sudositereputation.TestData.S3_PATH_MALICIOUS_DOMAINS
import com.sudoplatform.sudositereputation.s3.S3Exception
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoSiteReputationClient.update] using mocks and spies.
 *
 * @since 2021-01-05
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoSiteReputationClientUpdateTest : BaseTests() {

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
    fun `update() should call S3 client`() = runBlocking<Unit> {

        siteReputationClient.update()

        verify(mockS3Client).list(eq(S3_TOP_PATH), anyInt())
        verify(mockStorageProvider).readFileETag(MALICIOUS_DOMAINS_FILE)
        verify(mockS3Client).download(eq(S3_PATH_MALICIOUS_DOMAINS))
        verify(mockStorageProvider, times(2)).read(anyString())
        verify(mockStorageProvider).write(eq(MALICIOUS_DOMAINS_FILE), any())
        verify(mockStorageProvider).writeFileETag(eq(MALICIOUS_DOMAINS_FILE), any())
        verify(mockStorageProvider).write(eq(LAST_UPDATED_FILE), any())
    }

    @Test
    fun `update() should return null when s3 client download fails`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { download(anyString()) } doThrow S3Exception.DownloadException("mock")
        }

        siteReputationClient.update()

        verify(mockStorageProvider).readFileETag(anyString())
        verify(mockS3Client).list(anyString(), anyInt())
        verify(mockS3Client).download(anyString())
    }

    @Test
    fun `update() should throw when s3 client throws`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { download(anyString()) } doThrow IllegalStateException("mock")
        }

        shouldThrow<SudoSiteReputationException.UnknownException> {
            siteReputationClient.update()
        }

        verify(mockStorageProvider).readFileETag(anyString())
        verify(mockS3Client).list(anyString(), anyInt())
        verify(mockS3Client).download(anyString())
    }

    @Test
    fun `update() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { download(anyString()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            siteReputationClient.update()
        }

        verify(mockStorageProvider).readFileETag(anyString())
        verify(mockS3Client).list(anyString(), anyInt())
        verify(mockS3Client).download(anyString())
    }
}
