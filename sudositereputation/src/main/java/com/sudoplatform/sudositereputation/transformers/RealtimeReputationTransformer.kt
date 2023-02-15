package com.sudoplatform.sudositereputation.transformers

import com.sudoplatform.sudositereputation.graphql.fragment.Reputation
import com.sudoplatform.sudositereputation.graphql.type.ReputationStatus
import com.sudoplatform.sudositereputation.graphql.type.Scope
import com.sudoplatform.sudositereputation.types.RealtimeReputation

internal object RealtimeReputationTransformer {

    fun toReputationFromGraphQL(result: Reputation): RealtimeReputation {
        val isMalicious: RealtimeReputation.MaliciousState
        if (result.isMalicious != null && result.isMalicious == true) {
            isMalicious = RealtimeReputation.MaliciousState.MALICIOUS
        } else if (result.isMalicious != null && result.isMalicious == false) {
            isMalicious = RealtimeReputation.MaliciousState.NOTMALICIOUS
        } else {
            isMalicious = RealtimeReputation.MaliciousState.UNKNOWN
        }

        return RealtimeReputation(
            categories = result.categories().map { it.id().toInt() },
            confidence = result.confidence(),
            isMalicious = isMalicious,
            scope = result.scope()?.let { toScopeFromGraphQL(it) },
            status = toStatusFromGraphQL(result.status())
        )
    }

    fun toScopeFromGraphQL(scope: Scope): RealtimeReputation.Scope {
        return when (scope) {
            Scope.Domain -> RealtimeReputation.Scope.DOMAIN
            Scope.Path -> RealtimeReputation.Scope.PATH
        }
    }

    fun toStatusFromGraphQL(status: ReputationStatus): RealtimeReputation.Status {
        return when (status) {
            ReputationStatus.Success -> RealtimeReputation.Status.SUCCESS
            ReputationStatus.NotFound -> RealtimeReputation.Status.NOTFOUND
        }
    }
}
