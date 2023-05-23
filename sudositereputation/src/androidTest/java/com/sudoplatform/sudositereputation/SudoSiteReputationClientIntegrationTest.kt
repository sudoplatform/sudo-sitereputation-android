/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudositereputation.types.SiteReputation
import io.kotlintest.shouldBe
import java.time.Instant
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
    fun test_getReputation_returns_knownGoodSite() {
        // We need a test that the service tells us a site is good, but it's not ready
        // Remind us in a few weeks.
        // May 6, 2023
        val reminderDate = Instant.ofEpochSecond(1674151453)
        assert(reminderDate.isBefore(Instant.now()))
    }

    @Test
    fun test_getReputation_returns_unknownSite() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()

        val instance = createClient()
        val anonyomeReputation = instance.getSiteReputation("http://www.anonyome.com")
        // Our responses only contain items that are bad, so this will return UNKNOWN for now
        anonyomeReputation.status.shouldBe(SiteReputation.ReputationStatus.UNKNOWN)
    }

    @Test
    fun test_getReputation_returns_knownBadSite() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()

        val instance = createClient()
        val badSite = instance.getSiteReputation("http://malware.wicar.org/data/eicar.com")
        badSite.status.shouldBe(SiteReputation.ReputationStatus.MALICIOUS)
    }

    @Test
    fun testInvalidURLReturnsUnknownStatusOnInvalidURI() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()

        val instance = createClient()
        instance.getSiteReputation("foo").status.shouldBe(SiteReputation.ReputationStatus.UNKNOWN)
    }
}
