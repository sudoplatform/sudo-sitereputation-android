/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation.transformers

import androidx.annotation.VisibleForTesting
import com.sudoplatform.sudositereputation.s3.S3Client
import com.sudoplatform.sudositereputation.types.Ruleset
import org.json.JSONException
import org.json.JSONObject

/**
 * Transform from S3 types to site reptuation [Ruleset] and vice-versa.
 *
 * @since 2021-01-04
 */
internal object RulesetTransformer {

    // S3 metadata items. This is what the user metadata in the S3 object looks like.
    // {sudoplatformblob={"categoryEnum":"MALICIOUSDOMAIN","name.en":"uBlock Origin"}}
    @VisibleForTesting
    const val METADATA_BLOB = "sudoplatformblob"
    @VisibleForTesting
    const val METADATA_TYPE = "categoryEnum"
    @VisibleForTesting
    const val METADATA_CATEGORY_MALICIOUSDOMAIN = "MALICIOUSDOMAIN"
    const val METADATA_CATEGORY_MALWARE = "MALWARE"
    const val METADATA_CATEGORY_PHISHING = "PHISHING"

    fun toRulesetList(s3ObjectInfoList: List<S3Client.S3ObjectInfo>): List<Ruleset> {
        return s3ObjectInfoList.filter {
            val rulesetType = extractRulesetTypeFromMetadata(it.userMetadata)
            rulesetType != Ruleset.Type.UNKNOWN && rulesetType != Ruleset.Type.MALICIOUS_DOMAINS
        }.map {
            toRuleset(it)
        }
    }

    fun toRuleset(objectInfo: S3Client.S3ObjectInfo): Ruleset {
        return Ruleset(
            id = objectInfo.key,
            eTag = objectInfo.eTag,
            type = extractRulesetTypeFromMetadata(objectInfo.userMetadata),
            updatedAt = objectInfo.lastModified
        )
    }

    private fun extractRulesetTypeFromMetadata(userMetadata: Map<String, String>): Ruleset.Type {
        return userMetadata[METADATA_BLOB]?.let { blob ->
            try {
                JSONObject(blob).getString(METADATA_TYPE).toRulesetType()
            } catch (e: JSONException) {
                null
            }
        } ?: Ruleset.Type.UNKNOWN
    }

    fun String?.toRulesetType(): Ruleset.Type {
        return when (this?.trim()) {
            METADATA_CATEGORY_MALICIOUSDOMAIN -> Ruleset.Type.MALICIOUS_DOMAINS
            METADATA_CATEGORY_MALWARE -> Ruleset.Type.MALWARE
            METADATA_CATEGORY_PHISHING -> Ruleset.Type.PHISHING
            else -> Ruleset.Type.UNKNOWN
        }
    }
}
