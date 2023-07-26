/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import android.content.Context
import com.sudoplatform.sudologging.LogDriverInterface
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.DefaultLegacySiteReputationClient.Companion.LAST_UPDATED_FILE
import com.sudoplatform.sudositereputation.TestData.S3_OBJECTS
import com.sudoplatform.sudositereputation.TestData.USER_ID
import com.sudoplatform.sudositereputation.TestData.USER_SUBJECT
import com.sudoplatform.sudositereputation.reputation.ReputationProvider
import com.sudoplatform.sudositereputation.rules.ActualPropertyResetter
import com.sudoplatform.sudositereputation.rules.PropertyResetRule
import com.sudoplatform.sudositereputation.rules.PropertyResetter
import com.sudoplatform.sudositereputation.rules.TimberLogRule
import com.sudoplatform.sudositereputation.s3.S3Client
import com.sudoplatform.sudositereputation.storage.StorageProvider
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/**
 * Base class that sets up:
 * - [TimberLogRule]
 * - [PropertyResetRule]
 *
 * And provides convenient access to the [PropertyResetRule.before] via [PropertyResetter.before].
 */
internal abstract class BaseTests : PropertyResetter by ActualPropertyResetter() {
    @Rule
    @JvmField
    val timberLogRule = TimberLogRule()

    protected val mockContext by before {
        mock<Context>()
    }

    protected val mockLogDriver by before {
        mock<LogDriverInterface>().stub {
            on { logLevel } doReturn LogLevel.VERBOSE
        }
    }

    protected val mockLogger by before {
        Logger("mock", mockLogDriver)
    }

    protected val mockUserClient by before {
        mock<SudoUserClient>().stub {
            on { getUserName() } doReturn USER_ID
            on { getSubject() } doReturn USER_SUBJECT
        }
    }

    protected val mockS3Client by before {
        mock<S3Client>().stub {
            onBlocking {
                list(anyString(), any())
            } doReturn S3_OBJECTS
            onBlocking { download(anyString()) } doReturn ByteArray(42)
        }
    }

    protected val mockStorageProvider by before {
        mock<StorageProvider>().stub {
            on { readFileETag(anyString()) } doReturn null
            on { read(LAST_UPDATED_FILE) } doReturn "${System.currentTimeMillis()}".toByteArray()
        }
    }

    protected val mockReputationProvider by before {
        mock<ReputationProvider>().stub {
            onBlocking { checkIsUrlMalicious(anyString()) } doReturn false
        }
    }

    protected val siteReputationClient by before {
        DefaultLegacySiteReputationClient(
            context = mockContext,
            logger = mockLogger,
            sudoUserClient = mockUserClient,
            region = "region",
            bucket = "bucket",
            s3Client = mockS3Client,
            storageProvider = mockStorageProvider,
            reputationProvider = mockReputationProvider
        )
    }

    protected fun verifyMocksUsedInClientInit() = runBlocking<Unit> {
        verify(mockStorageProvider, atLeastOnce()).read(anyString())
    }
}
