/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import com.sudoplatform.sudositereputation.DefaultSiteReputationClient.Companion.MALICIOUS_DOMAINS_FILE
import com.sudoplatform.sudositereputation.DefaultSiteReputationClient.Companion.MALICIOUS_DOMAINS_SUBPATH
import com.sudoplatform.sudositereputation.DefaultSiteReputationClient.Companion.S3_TOP_PATH
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

    val S3_PATH_MALICIOUS_DOMAINS = "$S3_TOP_PATH/$MALICIOUS_DOMAINS_SUBPATH/$MALICIOUS_DOMAINS_FILE"

    val S3_REPUTATION_OBJECT_USER_METADATA = mapOf(
        RulesetTransformer.METADATA_BLOB to """{
            "${RulesetTransformer.METADATA_TYPE}": "${RulesetTransformer.METADATA_CATEGORY_MALICIOUSDOMAIN}"
        }"""
    )

    val S3_OBJECTS = listOf(
        S3Client.S3ObjectInfo(
            key = "maliciousdomain",
            eTag = "etag1",
            lastModified = Date(1L),
            userMetadata = S3_REPUTATION_OBJECT_USER_METADATA
        )
    )

    val MALICIOUS = setOf(
        "aboveandbelow.com.au",
        "wildnights.co.uk",
        "endurotanzania.co.tz",
        "tentandoserfitness.000webhostapp.com"
    )
    val SHOULD_NOT_BE_BLOCKED = setOf(
        "anonyome.com/about.js",
        "mysudo.com/support/foo.js",
        "brisbanetimes.com.au"
    )
}
