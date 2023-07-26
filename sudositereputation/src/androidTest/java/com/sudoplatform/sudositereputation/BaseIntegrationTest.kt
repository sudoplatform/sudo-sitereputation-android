/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudoentitlementsadmin.SudoEntitlementsAdminClient
import com.sudoplatform.sudoentitlementsadmin.types.Entitlement
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.TESTAuthenticationProvider
import io.kotlintest.shouldBe
import timber.log.Timber

internal fun String.toUrl() = "http://$this"

/**
 * Base class of the integration tests of the Sudo Site Reputation SDK.
 *
 * @since 2021-01-04
 */
abstract class BaseIntegrationTest {

    private val verbose = true
    private val logLevel = if (verbose) LogLevel.VERBOSE else LogLevel.INFO
    protected val logger = Logger("sr-test", AndroidUtilsLogDriver(logLevel))

    protected val context: Context = ApplicationProvider.getApplicationContext<Context>()

    protected val userClient by lazy {
        SudoUserClient.builder(context)
            .setNamespace("sr-client-test")
            .setLogger(logger)
            .build()
    }

    protected val keyManager by lazy {
        KeyManagerFactory(context).createAndroidKeyManager()
    }

    protected val entitlementsClient by lazy {
        SudoEntitlementsClient.builder()
            .setContext(context)
            .setSudoUserClient(userClient)
            .build()
    }

    protected val entitlementsAdminClient by lazy {
        val adminApiKey = readArgument("ADMIN_API_KEY", "api.key")
        SudoEntitlementsAdminClient.builder(context, adminApiKey).build()
    }

    protected fun readArgument(argumentName: String, fallbackFileName: String?): String {
        val argumentValue =
            InstrumentationRegistry.getArguments().getString(argumentName)?.trim()
        if (argumentValue != null) {
            return argumentValue
        }
        if (fallbackFileName != null) {
            return readTextFile(fallbackFileName)
        }
        throw IllegalArgumentException("$argumentName property not found")
    }

    private suspend fun registerUser() {
        userClient.isRegistered() shouldBe false

        val privateKey = readTextFile("register_key.private")
        val keyId = readTextFile("register_key.id")

        val authProvider = TESTAuthenticationProvider(
            name = "sr-client-test",
            privateKey = privateKey,
            publicKey = null,
            keyManager = keyManager,
            keyId = keyId
        )

        userClient.registerWithAuthenticationProvider(authProvider, "sr-client-test")
    }

    protected fun readTextFile(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use {
            it.readText().trim()
        }
    }

    protected fun readFile(fileName: String): ByteArray {
        return context.assets.open(fileName).use {
            it.readBytes()
        }
    }

    protected suspend fun signInAndRegisterUser() {
        if (!userClient.isRegistered()) {
            registerUser()
        }
        userClient.isRegistered() shouldBe true
        if (userClient.isSignedIn()) {
            userClient.getRefreshToken()?.let { userClient.refreshTokens(it) }
        } else {
            userClient.signInWithKey()
        }
        userClient.isSignedIn() shouldBe true
    }

    protected suspend fun applyAndRedeemEntitlements() {
        val username = userClient.getUserName()
        require(username != null) { "Username not found." }
        val entitlements = listOf(Entitlement("sudoplatform.sr.srUserEntitled", null, 1))
        entitlementsAdminClient.applyEntitlementsToUser(username, entitlements)
        entitlementsClient.redeemEntitlements()
    }

    protected fun clientConfigFilesPresent(): Boolean {
        val configFiles = context.assets.list("")?.filter { fileName ->
            fileName == "sudoplatformconfig.json" ||
                fileName == "register_key.private" ||
                fileName == "register_key.id"
        } ?: emptyList()
        Timber.d("config files present ${configFiles.size}")
        return configFiles.size == 3
    }
}
