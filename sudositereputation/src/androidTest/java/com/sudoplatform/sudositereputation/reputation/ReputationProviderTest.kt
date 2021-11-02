/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.reputation

import com.sudoplatform.sudositereputation.BaseIntegrationTest
import com.sudoplatform.sudositereputation.TestData.MALICIOUS
import com.sudoplatform.sudositereputation.TestData.SHOULD_NOT_BE_BLOCKED
import com.sudoplatform.sudositereputation.toUrl
import com.sudoplatform.sudositereputation.types.Ruleset
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber

private const val MAX_RULE_LOADING_MS = 40L
private const val MAX_URL_CHECKING_MS = 5L

/**
 * Test the operation of [DefaultReputationProvider] on a Android device or emulator.
 *
 * @since 2021-01-05
 */
class ReputationProviderTest : BaseIntegrationTest() {

    private val reputationProvider = DefaultReputationProvider(logger)

    private val maliciousDomainsFile by lazy {
        readFile("urlhaus-filter-domains-online.txt")
    }

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        reputationProvider.close()
        Timber.uprootAll()
    }

    @Test
    fun shouldBlockPrivacyViolatorUrls() = runBlocking<Unit> {
        reputationProvider.setRules(maliciousDomainsFile, Ruleset.Type.MALICIOUS_DOMAINS)

        for (testCase in MALICIOUS) {
            reputationProvider.checkIsUrlMalicious(testCase.toUrl()) shouldBe true
        }
    }

    @Test
    fun shouldNotBlockGoodUrls() = runBlocking<Unit> {
        reputationProvider.setRules(maliciousDomainsFile, Ruleset.Type.MALICIOUS_DOMAINS)

        for (testCase in SHOULD_NOT_BE_BLOCKED) {
            reputationProvider.checkIsUrlMalicious(testCase.toUrl()) shouldBe false
        }
    }

    // disabled for now because the CI machines are slow and this test often fails.
    /*
    @Test
    fun timingTest() = runBlocking<Unit> {
        val stopwatch = Stopwatch.createStarted()
        reputationProvider.setRules(maliciousDomainsFile, Ruleset.Type.MALICIOUS_DOMAINS)
        stopwatch.stop()
        println("Rules loading took $stopwatch")
        stopwatch.elapsed(TimeUnit.MILLISECONDS) shouldBeLessThan MAX_RULE_LOADING_MS

        stopwatch.reset()
        stopwatch.start()

        val allTestCases = MALICIOUS + SHOULD_NOT_BE_BLOCKED
        for (testCase in allTestCases) {
            reputationProvider.checkIsUrlMalicious(testCase.toUrl())
        }

        stopwatch.stop()
        println("Testing ${allTestCases.size} URLs took $stopwatch")
        stopwatch.elapsed(TimeUnit.MILLISECONDS) shouldBeLessThan MAX_URL_CHECKING_MS
    }
     */
}
