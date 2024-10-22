/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudositereputation.s3

import java.util.Date

/**
 * S3 client wrapper mainly used for providing an abstraction layer on top of AWS S3 SDK.
 * This is a clone of the class of the same name in SudoProfiles and enhanced to be able to list
 * the contents of an S3 bucket.
 *
 * @since 2021-01-04
 */
internal interface S3Client {

    companion object {
        private const val DEFAULT_LIMIT = 50
    }

    /**
     * AWS region hosting the S3 bucket.
     */
    val region: String

    /**
     * S3 bucket used by SudoSiteReputation service for storing rulesets.
     */
    val bucket: String

    /**
     * Downloads a blob from AWS S3.
     *
     * @param key AWS S3 key representing the location of the blob.
     */
    @Throws(S3Exception::class)
    suspend fun download(key: String): ByteArray

    data class S3ObjectInfo(
        val key: String,
        val userMetadata: Map<String, String>,
        val eTag: String,
        val lastModified: Date,
    )

    /**
     * Lists the objects from an AWS S3 bucket.
     *
     * @param path Path (prefix) of the objects to list.
     * @param limit The maximum number of objects to return in the list.
     * @return List of the objects information
     */
    @Throws(S3Exception::class)
    suspend fun list(path: String, limit: Int = DEFAULT_LIMIT): List<S3ObjectInfo>
}
