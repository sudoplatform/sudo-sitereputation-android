/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.transformers

import com.sudoplatform.sudositereputation.s3.S3Client
import com.sudoplatform.sudositereputation.transformers.RulesetTransformer.toRulesetType
import com.sudoplatform.sudositereputation.types.Ruleset
import io.kotlintest.matchers.collections.shouldBeOneOf
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.junit.Test
import java.util.Date
import java.util.Locale

/**
 * Test the operation of [RulesetTransformer].
 *
 * @since 2021-01-04
 */
class RulesetTransformerTest {

    @Test
    fun `transformer should convert all Ruleset types`() {
        val testCases = listOf(
            "MALICIOUSDOMAIN",
            "MALWARE",
            "PHISHING",
            "UNKNOWN",
        )
        val types = Ruleset.Type.values().toList()
        var unknownCount = 0
        testCases.forEach { testCase ->
            testCase.toRulesetType() shouldBeOneOf types
            testCase.lowercase(Locale.ROOT).toRulesetType() shouldBeOneOf types
            " $testCase ".toRulesetType() shouldBeOneOf types
            if (testCase.toRulesetType() == Ruleset.Type.UNKNOWN) {
                ++unknownCount
            }
        }
        unknownCount shouldNotBe types.size
    }

    @Test
    fun `transformer should convert bad Ruleset types to unknown`() {
        val testCases = listOf(
            "xMALICIOUSDOMAINS",
            "UNKNOWN",
            "UNKNOWNs",
            "",
        )
        testCases.forEach { testCase ->
            testCase.toRulesetType() shouldBe Ruleset.Type.UNKNOWN
            testCase.lowercase(Locale.ROOT).toRulesetType() shouldBe Ruleset.Type.UNKNOWN
            " $testCase ".toRulesetType() shouldBe Ruleset.Type.UNKNOWN
        }
    }

    @Test
    fun `transformer should convert S3 data to Ruleset`() {
        val s3ObjectInfo = listOf(
            S3Client.S3ObjectInfo(
                key = "key",
                eTag = "42",
                lastModified = Date(1L),
                userMetadata = mapOf(
                    RulesetTransformer.METADATA_BLOB to """{
                        "${RulesetTransformer.METADATA_TYPE}": "MALICIOUSDOMAIN"
                    }""",
                ),
            ),
            S3Client.S3ObjectInfo(
                key = "keyGood",
                eTag = "42",
                lastModified = Date(1L),
                userMetadata = mapOf(
                    RulesetTransformer.METADATA_BLOB to """{
                        "${RulesetTransformer.METADATA_TYPE}": "MALWARE"
                    }""",
                ),
            ),
            S3Client.S3ObjectInfo(
                key = "key2",
                eTag = "43",
                lastModified = Date(1L),
                userMetadata = mapOf(
                    RulesetTransformer.METADATA_BLOB to """{
                        "${RulesetTransformer.METADATA_TYPE}": "unsupported"
                    }""",
                ),
            ),
            S3Client.S3ObjectInfo(
                key = "key3",
                eTag = "44",
                lastModified = Date(1L),
                userMetadata = mapOf(
                    RulesetTransformer.METADATA_BLOB to "{",
                ),
            ),
        )
        val rulesetList = RulesetTransformer.toRulesetList(s3ObjectInfo)
        rulesetList shouldHaveSize 1
        with(rulesetList[0]) {
            id shouldBe "keyGood"
            eTag shouldBe "42"
            updatedAt.time shouldBe 1L
            type shouldBe Ruleset.Type.MALWARE
        }
    }
}
