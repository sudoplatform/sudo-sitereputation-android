package com.sudoplatform.sudositereputation.types

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class SiteReputationRuleList(
    val type: Ruleset.Type,
    val rules: MutableSet<SiteReputationRule>,
) : Parcelable
