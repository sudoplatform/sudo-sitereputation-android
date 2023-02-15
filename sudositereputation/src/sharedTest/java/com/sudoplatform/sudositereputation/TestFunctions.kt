/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldNotBe

/**
 * Test functions.
 *
 * @since 2021-01-04
 */
fun checkReputationList(reputationListBytes: ByteArray?) {
    reputationListBytes shouldNotBe null
    reputationListBytes!!.size shouldBeGreaterThan 10_000
    val reputationList = String(reputationListBytes)
    reputationList shouldStartWith "# Title: Online Malicious Domains Blocklist"
    reputationList shouldContain "# Homepage: https://gitlab.com/malware-filter/urlhaus-filter"
}
