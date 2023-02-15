/*
 * Copyright © 2021 - Anonyome Labs, Inc. - All rights reserved
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation.s3

import com.sudoplatform.sudoconfigmanager.ServiceCompatibilityInfo
import com.sudoplatform.sudoconfigmanager.SudoConfigManager
import com.sudoplatform.sudoconfigmanager.ValidationResult
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudositereputation.BaseTests
import com.sudoplatform.sudositereputation.SudoSiteReputationException
import com.sudoplatform.sudositereputation.logging.LogConstants
import io.kotlintest.shouldThrow
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test the handling of the JSON config items.
 *
 * @since 2021-01-04
 */
@RunWith(RobolectricTestRunner::class)
internal class S3ConfigurationTest : BaseTests() {

    private fun configManager(configJson: String): SudoConfigManager {
        return object : SudoConfigManager {
            override fun getConfigSet(namespace: String): JSONObject? {
                if (namespace == "siteReputationService") {
                    return JSONObject(configJson)
                }
                return null
            }

            /**
             * Validates the client configuration (sudoplatformconfig.json) against the currently deployed set of
             * backend services. If the client configuration is valid, i.e. the client is compatible will all deployed
             * backend services, then the call will complete with `success` result. If any part of the client
             * configuration is incompatible then a detailed information on the incompatible service will be
             * returned in `failure` result. See `SudoConfigManagerError.compatibilityIssueFound` for more details.
             *
             * @return validation result with the details of incompatible or deprecated service configurations.
             */
            override suspend fun validateConfig(): ValidationResult {
                return ValidationResult(listOf<ServiceCompatibilityInfo>(), listOf<ServiceCompatibilityInfo>())
            }
        }
    }

    @Test
    fun shouldThrowIfConfigMissing() {

        val logger = com.sudoplatform.sudologging.Logger(LogConstants.SUDOLOG_TAG, AndroidUtilsLogDriver(LogLevel.INFO))

        val noConfigJson = ""
        shouldThrow<SudoSiteReputationException.ConfigurationException> {
            readS3Configuration(mockContext, logger, configManager(noConfigJson))
        }

        val emptyConfigJson = "{}"
        shouldThrow<SudoSiteReputationException.ConfigurationException> {
            readS3Configuration(mockContext, logger, configManager(emptyConfigJson))
        }

        val missingRegionJson = """
            {
                "bucket": "ids-userdata-eml-dev-transientuserdatabucket0d043-5tkr1hts9sja"
            }
        """.trimIndent()

        shouldThrow<SudoSiteReputationException.ConfigurationException> {
            readS3Configuration(mockContext, logger, configManager(missingRegionJson))
        }

        val completeConfigJson = """
            {
                "region": "us-east-1",
                "bucket": "foo"
            }
        """.trimIndent()

        readS3Configuration(mockContext, logger, configManager(completeConfigJson))
    }
}
