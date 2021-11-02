/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.reputation

import com.sudoplatform.sudositereputation.SudoSiteReputationException
import com.sudoplatform.sudositereputation.types.Ruleset

/**
 * Provide reputation information.
 *
 * @since 2021-01-05
 */
internal interface ReputationProvider : AutoCloseable {

    /**
     * Set the rules the reputation service should use to determine the reputation of a site from a URL.
     *
     * @param reputationRulesBytes The reputation rules to use.
     */
    suspend fun setRules(reputationRulesBytes: ByteArray, rulesetType: Ruleset.Type)

    /**
     * Checks the host or domain in a URL to determine if it is listed as malicious.
     *
     * @param url The URL of the host or domain that should be checked against the current rulesets
     * @return true if the host or domain in the URL is malicious
     */
    @Throws(SudoSiteReputationException::class)
    suspend fun checkIsUrlMalicious(url: String): Boolean
}
