/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.storage

import androidx.annotation.VisibleForTesting
import java.io.File
import java.io.IOException

/**
 * Storage services are provided to the [SudoSiteReputationClient] by classes that implement
 * this interface.
 *
 * @since 2021-01-04
 */
interface StorageProvider {

    /**
     * Reads all the bytes from a file.
     *
     * @param fileName The name of the file without a path.
     * @return The contents of the file or null if the file does not exist.
     */
    @Throws(IOException::class)
    fun read(fileName: String): ByteArray?

    /**
     * Gets the [File] of a file.
     *
     * @param fileName The name of the file without a path.
     * @return The [File] of the named file.
     */
    @Throws(IOException::class)
    fun getFile(fileName: String): File

    /**
     * Writes all the bytes to a file.
     *
     * @param fileName The name of the file without a path.
     * @param data The contents of the file.
     */
    @Throws(IOException::class)
    fun write(fileName: String, data: ByteArray)

    /**
     * Delete a file.
     *
     * @param fileName The name of the file without a path.
     * @return true if the file was deleted, false if it didn't exist
     */
    @Throws(IOException::class)
    fun delete(fileName: String): Boolean

    /**
     * Delete all the files managed by the [StorageProvider].
     */
    @Throws(IOException::class)
    fun deleteFiles()

    /**
     * List all the files in the storage provider.
     *
     * @return [List] of file names.
     */
    @VisibleForTesting
    @Throws(IOException::class)
    fun listFiles(): List<String>

    /**
     * Reads the eTag of a file, returns null if the file does not exist.
     *
     * @param fileName The name of the file without a path.
     * @return The eTag of the file or null if the file does not exist.
     */
    @Throws(IOException::class)
    fun readFileETag(fileName: String): String?

    /**
     * Writes the eTag of a file.
     *
     * @param fileName The name of the file without a path.
     * @param eTag The eTag of the file.
     */
    @Throws(IOException::class)
    fun writeFileETag(fileName: String, eTag: String)

    /**
     * Delete the eTag of a file.
     *
     * @param fileName The name of the file without a path.
     * @return true if the file's eTag was deleted, false if it didn't exist
     */
    @Throws(IOException::class)
    fun deleteFileETag(fileName: String): Boolean

    /**
     * Delete all the file eTags managed by the [StorageProvider].
     */
    @Throws(IOException::class)
    fun deleteFileETags()
}
