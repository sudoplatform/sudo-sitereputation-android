/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudositereputation.TestData.MALICIOUS
import com.sudoplatform.sudositereputation.TestData.SHOULD_NOT_BE_BLOCKED
import com.sudoplatform.sudositereputation.storage.DefaultStorageProvider
import com.sudoplatform.sudositereputation.types.Ruleset
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.numerics.shouldBeGreaterThanOrEqual
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.util.Date

/**
 * Test the operation of the [SudoSiteReputationClient].
 *
 * @since 2021-01-04
 */
@RunWith(AndroidJUnit4::class)
class SudoSiteReputationClientIntegrationTest : BaseIntegrationTest() {

    private var siteReputationClient: SudoSiteReputationClient? = null
    private val storageProvider = DefaultStorageProvider(context)

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() = runBlocking<Unit> {
        if (clientConfigFilesPresent()) {
            siteReputationClient?.clearStorage()
        }
        Timber.uprootAll()
    }

    private fun createClient() = runBlocking<SudoSiteReputationClient> {
        siteReputationClient = SudoSiteReputationClient.builder()
            .setContext(context)
            .setSudoUserClient(userClient)
            .setStorageProvider(storageProvider)
            .setLogger(logger)
            .build()
        siteReputationClient!!.clearStorage()
        siteReputationClient!!
    }

    @Test
    fun shouldThrowIfRequiredItemsNotProvidedToBuilder() {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        // All required items not provided
        shouldThrow<NullPointerException> {
            SudoSiteReputationClient.builder().build()
        }

        // Context not provided
        shouldThrow<NullPointerException> {
            SudoSiteReputationClient.builder()
                .setSudoUserClient(userClient)
                .build()
        }

        // SudoUserClient not provided
        shouldThrow<NullPointerException> {
            SudoSiteReputationClient.builder()
                .setContext(context)
                .build()
        }
    }

    @Test
    fun shouldNotThrowIfTheRequiredItemsAreProvidedToBuilder() {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        SudoSiteReputationClient.builder()
            .setContext(context)
            .setSudoUserClient(userClient)
            .build()
    }

    /**
     * Test the happy path of site reputation operations, which is the normal flow a
     * user would be expected to exercise.
     */
    @Test
    fun completeFlowShouldSucceed() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        val client = createClient()

        val clientImpl = client as DefaultSiteReputationClient
        val rulesets = clientImpl.listRulesets()
        rulesets.size shouldBeGreaterThanOrEqual 1
        with(rulesets[0]) {
            type shouldBe Ruleset.Type.MALICIOUS_DOMAINS
            id shouldNotBe ""
            eTag shouldNotBe ""
            updatedAt.time shouldBeGreaterThan 0L
        }

        // Prior to update there should be no rulesets loaded
        client.lastUpdatePerformedAt shouldBe null
        shouldThrow<SudoSiteReputationException.RulesetNotFoundException> {
            client.getSiteReputation(MALICIOUS.first())
        }

        // After update there should be a ruleset that will declare sites as malicious
        val beforeUpdate = Date()
        client.update()
        client.lastUpdatePerformedAt shouldNotBe null
        (client.lastUpdatePerformedAt?.before(beforeUpdate) ?: true) shouldBe false
        for (url in MALICIOUS) {
            client.getSiteReputation(url).isMalicious shouldBe true
        }
        for (url in SHOULD_NOT_BE_BLOCKED) {
            client.getSiteReputation(url).isMalicious shouldBe false
        }

        // After close there should be no rulesets loaded so nothing will be malicious
        client.close()
        client.lastUpdatePerformedAt shouldNotBe null
        (client.lastUpdatePerformedAt?.time ?: 0L) shouldBeGreaterThan 0L
        for (url in MALICIOUS + SHOULD_NOT_BE_BLOCKED) {
            client.getSiteReputation(url).isMalicious shouldBe false
        }

        // After clearStorage all the cached data should be gone
        client.clearStorage()
        client.lastUpdatePerformedAt shouldBe null
    }
}
