/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.s3

import com.sudoplatform.sudositereputation.BaseIntegrationTest
import com.sudoplatform.sudositereputation.DefaultSiteReputationClient
import com.sudoplatform.sudositereputation.checkReputationList
import io.kotlintest.matchers.collections.shouldHaveAtLeastSize
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

/**
 * Test the operation of the [DefaultS3Client].
 *
 * @since 2021-01-04
 */
class S3ClientIntegrationTest : BaseIntegrationTest() {

    private lateinit var s3Client: S3Client

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())

        if (clientConfigFilesPresent()) {
            val config = readS3Configuration(context, logger)
            s3Client = DefaultS3Client(
                context = context,
                sudoUserClient = userClient,
                logger = logger,
                bucket = config.bucket,
                region = config.region
            )
        }
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    @Test
    fun listShouldReturnObjectInfo() = runBlocking<Unit> {

        assumeTrue(clientConfigFilesPresent())

        signInAndRegisterUser()

        val objects = s3Client.list(DefaultSiteReputationClient.S3_TOP_PATH)
        objects shouldHaveAtLeastSize 1
        objects.forEach { println(it) }
    }

    @Test
    fun downloadShouldGetObject() = runBlocking<Unit> {

        assumeTrue(clientConfigFilesPresent())

        signInAndRegisterUser()

        val objects = s3Client.list(DefaultSiteReputationClient.S3_TOP_PATH)

        objects shouldHaveAtLeastSize 1

        checkReputationList(s3Client.download(objects.first().key))
    }
}
