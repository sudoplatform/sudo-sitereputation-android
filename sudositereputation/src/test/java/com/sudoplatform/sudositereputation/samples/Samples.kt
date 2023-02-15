/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation.samples

import android.content.Context
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudositereputation.SudoSiteReputationClient
import com.sudoplatform.sudositereputation.SudoSiteReputationException
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

/**
 * These are sample snippets of code that are included in the generated documentation. They are
 * placed here in the test code so that at least we know they will compile.
 *
 * @since 2021-01-04
 */
@RunWith(RobolectricTestRunner::class)
@Suppress("UNUSED_VARIABLE")
class Samples {

    @Test
    fun mockTest() {
        // Just to keep junit happy
    }

    private val context = mock<Context>()

    fun buildClient() {
        // This is how to construct the SudoSiteReputationClient

        // Create a logger for any messages or errors
        val logger = Logger("MyApplication", AndroidUtilsLogDriver(LogLevel.INFO))

        // Create an instance of SudoUserClient to perform registration and sign in.
        val sudoUserClient = SudoUserClient.builder(context)
            .setNamespace("com.mycompany.myapplication")
            .setLogger(logger)
            .build()

        // Create an instance of SudoSiteReputationClient block advertisers and trackers
        val sudoAdTrackerBlocker = SudoSiteReputationClient.builder()
            .setContext(context)
            .setSudoUserClient(sudoUserClient)
            .setLogger(logger)
            .build()
    }

    private lateinit var client: SudoSiteReputationClient

    // This function hides the GlobalScope from the code used in the documentation. The use
    // of GlobalScope is not something that should be recommended in the code samples.
    private fun launch(
        block: suspend CoroutineScope.() -> Unit
    ) = GlobalScope.launch { block.invoke(GlobalScope) }

    fun update() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    client.update()
                }
            } catch (e: SudoSiteReputationException) {
                // Handle/notify user of exception
            }
        }
    }

    fun lastUpdatePerformedAt() {
        launch {
            try {
                val yesterday = Calendar.getInstance().apply {
                    add(Calendar.HOUR_OF_DAY, -24)
                }
                if (client.lastUpdatePerformedAt?.before(yesterday.time) == true) {
                    // Reputation rulesets are more than 24 hours old or are missing, update them.
                    withContext(Dispatchers.IO) {
                        client.update()
                    }
                }
            } catch (e: SudoSiteReputationException) {
                // Handle/notify user of exception
            }
        }
    }

    fun getSiteReputation() {
        launch {
            val siteReputation = withContext(Dispatchers.IO) {
                client.getSiteReputation(
                    url = "http://somedodgyhost.com/somewhere"
                )
            }
            if (siteReputation.isMalicious) {
                // URL should not be loaded
            }
        }
    }

    fun clearStorage() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    client.clearStorage()
                }
            } catch (e: SudoSiteReputationException) {
                // Handle/notify user of exception
            }
        }
    }
}
