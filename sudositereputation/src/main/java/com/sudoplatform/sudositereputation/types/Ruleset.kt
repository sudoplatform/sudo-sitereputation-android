/*
 * Copyright 2021 Anonyome Labs Inc. All rights reserved.
 */
package com.sudoplatform.sudositereputation.types

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date

/**
 * A set of rules that provide:
 * - rules to provide a reputation rating of a web site
 *
 * @since 2021-01-04
 */
@Parcelize
internal data class Ruleset(
    /** The unique identifier of the ruleset */
    val id: String,
    /** The type of the ruleset */
    val type: Type,
    /** The eTag that is used to detect out of date rulesets */
    val eTag: String,
    /** When this ruleset was last updated */
    val updatedAt: Date,
) : Parcelable {
    enum class Type {
        MALICIOUS_DOMAINS,
        MALWARE,
        PHISHING,
        UNKNOWN
    }
}
