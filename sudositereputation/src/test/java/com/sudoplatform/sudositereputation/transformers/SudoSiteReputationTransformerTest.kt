/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.transformers

import com.sudoplatform.sudositereputation.graphql.fragment.Reputation
import com.sudoplatform.sudositereputation.graphql.type.ReputationStatus
import com.sudoplatform.sudositereputation.types.SiteReputation
import io.kotlintest.shouldBe
import org.junit.Test

class SudoSiteReputationTransformerTest {

    @Test
    fun `transforms graphQL response correctly`() {
        val input = Reputation(
            "Reputation",
            ReputationStatus.NOTMALICIOUS
        )

        val output = SudoSiteReputationTransformer.toReputationFromGraphQL(input)
        output.status shouldBe SiteReputation.ReputationStatus.NOTMALICIOUS
    }
}
