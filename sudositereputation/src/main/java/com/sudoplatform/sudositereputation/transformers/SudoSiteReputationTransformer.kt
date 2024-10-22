package com.sudoplatform.sudositereputation.transformers

import com.sudoplatform.sudositereputation.graphql.fragment.Reputation
import com.sudoplatform.sudositereputation.graphql.type.ReputationStatus
import com.sudoplatform.sudositereputation.types.SiteReputation

internal object SudoSiteReputationTransformer {

    fun toReputationFromGraphQL(result: Reputation): SiteReputation {
        val reputationStatus: SiteReputation.ReputationStatus =
            when (result.reputationStatus) {
                ReputationStatus.MALICIOUS -> SiteReputation.ReputationStatus.MALICIOUS
                ReputationStatus.NOTMALICIOUS -> SiteReputation.ReputationStatus.NOTMALICIOUS
                else -> SiteReputation.ReputationStatus.UNKNOWN
            }

        val categories = result.categories

        return SiteReputation(
            status = reputationStatus,
            categories = categories,
        )
    }
}
