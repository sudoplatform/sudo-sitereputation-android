/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation

/**
 * Defines the exceptions thrown by the methods of the [SudoSiteReputationClient].
 *
 * @property message Accompanying message for the exception.
 * @property cause The cause for the exception.
 * @since 2021-01-04
 */
sealed class SudoSiteReputationException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause) {
    /** A configuration item that is needed is missing */
    class ConfigurationException(message: String? = null, cause: Throwable? = null) :
        SudoSiteReputationException(message = message, cause = cause)
    class RulesetNotFoundException(message: String? = null, cause: Throwable? = null) :
        SudoSiteReputationException(message = message, cause = cause)
    class DataFormatException(message: String? = null, cause: Throwable? = null) :
        SudoSiteReputationException(message = message, cause = cause)
    class UnauthorizedUserException(message: String? = null, cause: Throwable? = null) :
        SudoSiteReputationException(message = message, cause = cause)
    class FailedException(message: String? = null, cause: Throwable? = null) :
        SudoSiteReputationException(message = message, cause = cause)
    class UnknownException(cause: Throwable) :
        SudoSiteReputationException(cause = cause)
}
