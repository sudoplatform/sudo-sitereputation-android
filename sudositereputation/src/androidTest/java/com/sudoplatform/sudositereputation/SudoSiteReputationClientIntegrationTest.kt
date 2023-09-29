/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudositereputation.types.SiteReputation
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the operation of the [SudoSiteReputationClient].
 *
 * @since 2021-01-04
 */
@RunWith(AndroidJUnit4::class)
class SudoSiteReputationClientIntegrationTest : BaseIntegrationTest() {

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() = runBlocking<Unit> {
        Timber.uprootAll()
    }

    private fun createClient(): SudoSiteReputationClient {
        return SudoSiteReputationClient.builder()
            .setContext(context)
            .setLogger(logger)
            .setSudoUserClient(userClient)
            .build()
    }

    @Test
    fun test_getReputation_returns_notMalicous() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        applyAndRedeemEntitlements()

        val instance = createClient()
        val anonyomeReputation = instance.getSiteReputation("http://www.anonyome.com")
        anonyomeReputation.status.shouldBe(SiteReputation.ReputationStatus.NOTMALICIOUS)
        anonyomeReputation.categories.shouldBe(emptyList())
    }

    @Test
    fun test_getReputation_returns_knownBadSite() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        applyAndRedeemEntitlements()

        val instance = createClient()
        val badSite = instance.getSiteReputation("http://malware.wicar.org/data/eicar.com")
        badSite.status.shouldBe(SiteReputation.ReputationStatus.MALICIOUS)
        badSite.categories.shouldBe(listOf("MALWARE"))
    }

    @Test
    fun testInvalidURLReturnsUnknownStatusOnInvalidURI() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        applyAndRedeemEntitlements()

        val instance = createClient()
        // TODO: This test feels like it's returning the wrong result, junk url's seem like "unknown" to me.
        // Maybe junk URL's should throw an error.
        val reputation = instance.getSiteReputation("BoogerAids AidsBooger")
        reputation.status.shouldBe(SiteReputation.ReputationStatus.NOTMALICIOUS)
        reputation.categories.shouldBe(emptyList())
    }
}
