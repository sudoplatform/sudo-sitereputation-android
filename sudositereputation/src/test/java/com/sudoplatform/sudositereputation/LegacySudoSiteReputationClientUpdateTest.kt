/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import com.sudoplatform.sudositereputation.DefaultLegacySiteReputationClient.Companion.BASE_RULESET_FILENAME
import com.sudoplatform.sudositereputation.DefaultLegacySiteReputationClient.Companion.LAST_UPDATED_FILE
import com.sudoplatform.sudositereputation.DefaultLegacySiteReputationClient.Companion.MALWARE_DOMAINS_SUBPATH
import com.sudoplatform.sudositereputation.DefaultLegacySiteReputationClient.Companion.S3_TOP_PATH
import com.sudoplatform.sudositereputation.s3.S3Exception
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [LegacySudoSiteReputationClient.update] using mocks and spies.
 *
 * @since 2021-01-05
 */
@RunWith(RobolectricTestRunner::class)
internal class LegacySudoSiteReputationClientUpdateTest : BaseTests() {

    @After
    fun fini() {
        verifyMocksUsedInClientInit()
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockS3Client,
            mockStorageProvider,
            mockReputationProvider,
        )
    }

    @Test
    fun `update() should call S3 client`() = runBlocking<Unit> {
        siteReputationClient.update()

        val fileName = "$BASE_RULESET_FILENAME-$MALWARE_DOMAINS_SUBPATH.txt"
        verify(mockReputationProvider, atLeast(1)).close()
        verify(mockStorageProvider).deleteFiles()
        verify(mockS3Client).list(eq(S3_TOP_PATH), anyInt())
        verify(mockStorageProvider).readFileETag(fileName)
        verify(mockS3Client).download(eq("malware"))
        verify(mockStorageProvider, times(6)).read(anyString())
        verify(mockStorageProvider).write(eq(fileName), any())
        verify(mockStorageProvider).writeFileETag(eq(fileName), any())
        verify(mockStorageProvider).write(eq(LAST_UPDATED_FILE), any())
    }

    @Test
    fun `update() should return null when s3 client download fails`() = runBlocking<Unit> {
        mockS3Client.stub {
            onBlocking { download(anyString()) } doThrow S3Exception.DownloadException("mock")
        }

        siteReputationClient.update()

        verify(mockReputationProvider).close()
        verify(mockStorageProvider).deleteFiles()
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

        verify(mockReputationProvider).close()
        verify(mockStorageProvider).deleteFiles()
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

        verify(mockReputationProvider).close()
        verify(mockStorageProvider).deleteFiles()
        verify(mockStorageProvider).readFileETag(anyString())
        verify(mockS3Client).list(anyString(), anyInt())
        verify(mockS3Client).download(anyString())
    }
}
