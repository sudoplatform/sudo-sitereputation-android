/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.reputation

import android.net.Uri
import com.sudoplatform.sudologging.Logger
import java.io.ByteArrayInputStream
import java.util.concurrent.CancellationException

private const val COMMENT = '#'

/**
 * Default implementation of a provider of site reputation.
 *
 * @since 2021-01-05
 */
internal class DefaultReputationProvider(private val logger: Logger) : ReputationProvider {

    private var maliciousDomains = mutableSetOf<String>()

    override suspend fun setRules(reputationRulesBytes: ByteArray) {
        try {
            maliciousDomains.clear()
            ByteArrayInputStream(reputationRulesBytes).bufferedReader().use { reader ->
                reader.readLines()
                    .map { line ->
                        line.trim()
                    }
                    .filter { line ->
                        line.isNotBlank() && !line.startsWith(COMMENT)
                    }
                    .forEach { line ->
                        maliciousDomains.add(line)
                    }
            }
            logger.info("Reputation provider rules set with ${maliciousDomains.size} entries.")
        } catch (e: CancellationException) {
            // Never suppress this exception it's used by coroutines to cancel outstanding work
            throw e
        }
    }

    override fun close() {
        maliciousDomains.clear()
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
        return maliciousDomains.contains(host)
    }
}
