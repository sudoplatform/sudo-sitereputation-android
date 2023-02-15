/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudositereputation.types.RealtimeReputation
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the operation of the [RealtimeSiteReputationClient].
 *
 * @since 2021-01-04
 */
@RunWith(AndroidJUnit4::class)
class RealtimeSiteReputationClientIntegrationTest : BaseIntegrationTest() {

    private var siteReputationClient: SudoSiteReputationClient? = null

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() = runBlocking<Unit> {
        Timber.uprootAll()
    }

    private fun createClient(): RealtimeSiteReputationClient {
        return RealtimeSiteReputationClient.builder()
            .setContext(context)
            .setLogger(logger)
            .setSudoUserClient(userClient)
            .build()
    }

    @Test
    fun test_getReputation_returns_knownGoodSite() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()

        val instance = createClient()
        val anonyomeReputation = instance.getSiteReputation("http://www.anonyome.com")
        anonyomeReputation.isMalicious.shouldBe(RealtimeReputation.MaliciousState.NOTMALICIOUS)
        // This list should contain `[9,34]`.
        anonyomeReputation.categories.shouldHaveSize(2)
    }

    @Test
    fun test_getReputation_returns_knownBadSite() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()

        val instance = createClient()
        val badSite = instance.getSiteReputation("http://malware.wicar.org/data/eicar.com")
        badSite.isMalicious.shouldBe(RealtimeReputation.MaliciousState.MALICIOUS)
        // This list should contain `[35,39]`.
        badSite.categories.shouldHaveSize(2)
    }

    @Test
    fun testInvalidURLReturnsErrorOnInvalidURI() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()

        val instance = createClient()
        shouldThrow<SudoSiteReputationException.FailedException> {
            instance.getSiteReputation("foo")
        }
    }
}
