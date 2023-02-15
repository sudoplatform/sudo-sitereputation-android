/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation.s3

import com.sudoplatform.sudouser.IdGenerator
import java.util.Locale
import java.util.UUID

/**
 * Generates unique identifiers for S3 objects
 *
 * @since 2021-01-04
 */
internal class DefaultIdGenerator : IdGenerator {
    override fun generateId(): String {
        return UUID.randomUUID().toString().uppercase(Locale.ROOT)
    }
}
