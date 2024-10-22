/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation.types

/**
 * Information about the reputation of a site.
 *
 * @since 2021-01-05
 */
data class LegacySiteReputation(
    /** True if the host or domains is in the list of malicious sites */
    val isMalicious: Boolean,
)
