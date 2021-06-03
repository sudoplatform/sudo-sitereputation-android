/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation

import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudositereputation.s3.S3Exception
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoSiteReputationClient.listRulesets] using mocks and spies
 * even though it's internal it is good to test it separately.
 *
 * @since 2021-01-04
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoSiteReputationClientListRulesetsTest : BaseTests() {

    @After
    fun fini() {
        verifyMocksUsedInClientInit()
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockS3Client,
            mockStorageProvider
        )
        runBlocking {
            siteReputationClient.clearStorage()
        }
    }

    @Test
    fun `listRulesets() should call S3 client`() = runBlocking<Unit> {

        siteReputationClient.listRulesets() shouldHaveSize 1

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultSiteReputationClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should return none when S3 client does`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doReturn emptyList()
        }

        siteReputationClient.listRulesets() shouldHaveSize 0

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultSiteReputationClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should throw when s3 client throws`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doThrow S3Exception.DownloadException("mock")
        }

        shouldThrow<SudoSiteReputationException.FailedException> {
            siteReputationClient.listRulesets()
        }

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultSiteReputationClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should throw when s3 client gets bad metadata`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doThrow S3Exception.MetadataException("mock")
        }

        shouldThrow<SudoSiteReputationException.DataFormatException> {
            siteReputationClient.listRulesets()
        }

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultSiteReputationClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            siteReputationClient.listRulesets()
        }

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultSiteReputationClient.S3_TOP_PATH), any())
    }
}
