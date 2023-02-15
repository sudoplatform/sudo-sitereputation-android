/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.transformers

import com.sudoplatform.sudositereputation.graphql.fragment.Reputation
import com.sudoplatform.sudositereputation.graphql.type.ReputationStatus
import com.sudoplatform.sudositereputation.graphql.type.Scope
import com.sudoplatform.sudositereputation.types.RealtimeReputation
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import org.junit.Test

/**
 * Test the operation of [RealtimeSiteReputationTransformer].
 *
 * @since 2023-01-18
 */
class RealtimeReputationTransformerTest {

    @Test
    fun `transforms scope correctly`() {
        RealtimeReputationTransformer.toScopeFromGraphQL(Scope.Domain) shouldBe RealtimeReputation.Scope.DOMAIN
        RealtimeReputationTransformer.toScopeFromGraphQL(Scope.Path) shouldBe RealtimeReputation.Scope.PATH
    }

    @Test
    fun `transforms status correctly`() {
        RealtimeReputationTransformer.toStatusFromGraphQL(ReputationStatus.NotFound) shouldBe RealtimeReputation.Status.NOTFOUND
        RealtimeReputationTransformer.toStatusFromGraphQL(ReputationStatus.Success) shouldBe RealtimeReputation.Status.SUCCESS
    }

    @Test
    fun `transforms graphQL response correctly`() {
        val category1 = Reputation.Category("", "1")
        val category2 = Reputation.Category("", "2")

        val input = Reputation(
            "Reputation",
            listOf(
                category1,
                category2
            ),
            Scope.Domain,
            ReputationStatus.Success,
            1.0,
            100,
            false
        )

        val output = RealtimeReputationTransformer.toReputationFromGraphQL(input)
        output.categories shouldContainExactly listOf(1, 2)
        output.scope shouldBe RealtimeReputation.Scope.DOMAIN
        output.status shouldBe RealtimeReputation.Status.SUCCESS
        output.confidence shouldBe 1.0
        output.isMalicious shouldBe RealtimeReputation.MaliciousState.NOTMALICIOUS
    }
}
