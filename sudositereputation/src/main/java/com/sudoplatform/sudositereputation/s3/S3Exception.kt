/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.s3

internal sealed class S3Exception(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause) {

    /**
     * Exception thrown when error occurs downloading items from S3 using the TransferListener
     */
    class DownloadException(message: String? = null, cause: Throwable? = null) :
        S3Exception(message = message, cause = cause)

    /**
     * Exception thrown when an S3 object is missing crucial metadata
     */
    class MetadataException(message: String? = null, cause: Throwable? = null) :
        S3Exception(message = message, cause = cause)
}
