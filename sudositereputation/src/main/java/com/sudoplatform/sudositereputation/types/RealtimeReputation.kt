package com.sudoplatform.sudositereputation.types

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize

public data class RealtimeReputation(
    /** Categories assigned to this result */
    val categories: List<Int>,
    /** confidence in the assigned rating */
    val confidence: Double?,
    /** The eTag that is used to detect out of date rulesets */
    val isMalicious: MaliciousState,
    /** scope of the search */
    val scope: Scope?,
    /** status of the search */
    val status: Status,
) : Parcelable {
    enum class Scope {
        /** matched on domain */
        DOMAIN,
        /** matched on path */
        PATH
    }
    enum class Status {
        /** URI found in dataset */
        SUCCESS,
        /** URI not found */
        NOTFOUND
    }

    enum class MaliciousState {
        /** site is known to be malicious */
        MALICIOUS,
        /** site is not known to be malicious */
        NOTMALICIOUS,
        /** no site data available to make a determination */
        UNKNOWN
    }
}
