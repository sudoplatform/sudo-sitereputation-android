/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation.s3

import android.content.Context
import com.sudoplatform.sudositereputation.SudoSiteReputationException
import com.sudoplatform.sudoconfigmanager.DefaultSudoConfigManager
import com.sudoplatform.sudoconfigmanager.SudoConfigManager
import com.sudoplatform.sudologging.Logger
import org.json.JSONException

private const val CONFIG_IDENTITY_SERVICE = "identityService"
private const val CONFIG_REGION = "region"
private const val CONFIG_STATIC_DATA_BUCKET = "staticDataBucket"

internal data class S3Configuration(
    val region: String,
    val bucket: String
)

/**
 * Read the S3 configuration elements from the Sudo Configuration Manager
 */
@Throws(SudoSiteReputationException.ConfigurationException::class)
internal fun readS3Configuration(
    context: Context,
    logger: Logger,
    configManager: SudoConfigManager = DefaultSudoConfigManager(context, logger)
): S3Configuration {

    val preamble = "sudoplatformconfig.json does not contain"
    val postamble = "the $CONFIG_IDENTITY_SERVICE stanza"

    val identityConfig = try {
        configManager.getConfigSet(CONFIG_IDENTITY_SERVICE)
    } catch (e: JSONException) {
        throw SudoSiteReputationException.ConfigurationException("$preamble $postamble", e)
    }
    identityConfig ?: throw SudoSiteReputationException.ConfigurationException("$preamble $postamble")

    val region = try {
        identityConfig.getString(CONFIG_REGION)
    } catch (e: JSONException) {
        throw SudoSiteReputationException.ConfigurationException("$preamble $CONFIG_REGION in $postamble", e)
    }

    val bucket = try {
        identityConfig.getString(CONFIG_STATIC_DATA_BUCKET)
    } catch (e: JSONException) {
        throw SudoSiteReputationException.ConfigurationException("$preamble $CONFIG_STATIC_DATA_BUCKET in $postamble", e)
    }

    return S3Configuration(region, bucket)
}
