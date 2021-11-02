/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.reputation

import android.net.Uri
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.types.Ruleset
import com.sudoplatform.sudositereputation.types.SiteReputationRule
import com.sudoplatform.sudositereputation.types.SiteReputationRuleList
import java.io.ByteArrayInputStream
import java.util.concurrent.CancellationException

private const val COMMENT = '#'

/**
 * Default implementation of a provider of site reputation.
 *
 * @since 2021-01-05
 */
internal class DefaultReputationProvider(private val logger: Logger) : ReputationProvider {

    private var ruleLists = mutableListOf<SiteReputationRuleList>()

    override suspend fun setRules(reputationRulesBytes: ByteArray, rulesetType: Ruleset.Type) {
        try {
            val matchingRuleList = ruleLists.firstOrNull { it.type == rulesetType }
            if (matchingRuleList != null) {
                ruleLists.remove(matchingRuleList)
            }
            val ruleList = SiteReputationRuleList(rulesetType, mutableSetOf<SiteReputationRule>())
            ruleLists.add(ruleList)
            ByteArrayInputStream(reputationRulesBytes).bufferedReader().use { reader ->
                val lines = reader.readLines()
                lines
                    .map { line ->
                        line.trim()
                    }
                    .filter { line ->
                        line.isNotBlank() && !line.startsWith(COMMENT)
                    }
                    .forEach { line ->
                        ruleList.rules.add(SiteReputationRule(line))
                    }
            }
        } catch (e: CancellationException) {
            // Never suppress this exception it's used by coroutines to cancel outstanding work
            throw e
        }
    }

    override fun close() {
        ruleLists.clear()
    }

    override suspend fun checkIsUrlMalicious(url: String): Boolean {
        var uri = Uri.parse(url.trim())
        if (uri.scheme == null) {
            // There is no http/https on the front
            uri = Uri.parse("scheme://$url")
        }
        val host = uri.host
        if (host.isNullOrBlank()) {
            return false
        }
        for (list in ruleLists) {
            if (list.rules.any { it.host == host }) {
                return true
            }
        }
        return false
    }
}
