/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import com.sudoplatform.sudositereputation.s3.S3Client
import com.sudoplatform.sudositereputation.transformers.RulesetTransformer
import java.util.Date
import java.util.UUID
/**
 * Data common to many tests.
 *
 * @since 2021-01-04
 */
internal object TestData {

    const val USER_ID = "slartibartfast"
    val USER_SUBJECT = UUID.randomUUID().toString()

    val S3_REPUTATION_OBJECT_USER_METADATA_MALWARE = mapOf(
        RulesetTransformer.METADATA_BLOB to """{
            "${RulesetTransformer.METADATA_TYPE}": "${RulesetTransformer.METADATA_CATEGORY_MALWARE}"
        }""",
    )

    val S3_REPUTATION_OBJECT_USER_METADATA_PHISHING = mapOf(
        RulesetTransformer.METADATA_BLOB to """{
            "${RulesetTransformer.METADATA_TYPE}": "${RulesetTransformer.METADATA_CATEGORY_PHISHING}"
        }""",
    )

    val S3_REPUTATION_OBJECT_USER_METADATA_MALICIOUSDOMAIN = mapOf(
        RulesetTransformer.METADATA_BLOB to """{
            "${RulesetTransformer.METADATA_TYPE}": "${RulesetTransformer.METADATA_CATEGORY_MALICIOUSDOMAIN}"
        }""",
    )

    val S3_OBJECTS = listOf(
        S3Client.S3ObjectInfo(
            key = "malware",
            eTag = "etag1",
            lastModified = Date(1L),
            userMetadata = S3_REPUTATION_OBJECT_USER_METADATA_MALWARE,
        ),
    )

    val MALICIOUS = setOf(
        "dezcom.com",
        "brightstarshop.com",
        "endurotanzania.co.tz",
        "tentandoserfitness.000webhostapp.com",
    )
    val SHOULD_NOT_BE_BLOCKED = setOf(
        "anonyome.com/about.js",
        "mysudo.com/support/foo.js",
        "brisbanetimes.com.au",
    )
}
