package com.sudoplatform.sudositereputation.types

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
public data class SiteReputation(
    /** status of the search */
    val status: ReputationStatus,
    val categories: List<String>,
) : Parcelable {

    enum class ReputationStatus {
        /** site is known to be malicious */
        MALICIOUS,

        /** site is not known to be malicious */
        NOTMALICIOUS,

        /** no site data available to make a determination */
        UNKNOWN,
    }
}
