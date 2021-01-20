/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.storage

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val PACKAGE = "com.sudoplatform.sudositereputation"

/**
 * Directory in which cached rules files will be stored. This is a value that shouldn't clash with
 * one chosen by the consuming app.
 */
private const val CACHE_SUBDIR = "$PACKAGE.cache"

/**
 * Directory in which cached file eTags will be stored. This is a value that shouldn't clash with
 * one chosen by the consuming app.
 */
private const val ETAG_SUBDIR = "$PACKAGE.etag"

/**
 * Default implementation of [StorageProvider] that uses private local storage.
 *
 * @since 2021-01-04
 */
internal class DefaultStorageProvider(private val context: Context) : StorageProvider {

    private val cacheDir = File(context.cacheDir, CACHE_SUBDIR)
    private val eTagDir = File(context.cacheDir, ETAG_SUBDIR)

    private fun ensureDirsExist() {
        if (!cacheDir.exists()) {
            cacheDir.mkdir()
        }
        if (!eTagDir.exists()) {
            eTagDir.mkdir()
        }
    }

    override fun getFile(fileName: String) = File(cacheDir, fileName)

    override fun read(fileName: String): ByteArray? {
        ensureDirsExist()
        val file = getFile(fileName)
        if (file.exists() && file.canRead()) {
            return file.readBytes()
        }
        return null
    }

    override fun write(fileName: String, data: ByteArray) {
        ensureDirsExist()
        getFile(fileName).writeBytes(data)
    }

    override fun delete(fileName: String): Boolean {
        val file = getFile(fileName)
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    override fun deleteFiles() {
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            cacheDir.mkdir()
        }
    }

    override fun listFiles(): List<String> {
        if (cacheDir.exists()) {
            return cacheDir.listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                ?: emptyList()
        }
        return emptyList()
    }

    private fun getETagFile(fileName: String) = File(eTagDir, fileName)

    override fun readFileETag(fileName: String): String? {
        ensureDirsExist()
        val eTagFile = getETagFile(fileName)
        if (eTagFile.exists() && eTagFile.canRead()) {
            FileInputStream(eTagFile).bufferedReader().use { reader ->
                return reader.readText().trim()
            }
        }
        return null
    }

    override fun writeFileETag(fileName: String, eTag: String) {
        ensureDirsExist()
        FileOutputStream(getETagFile(fileName)).bufferedWriter().use { writer ->
            writer.write(eTag.trim())
        }
    }

    override fun deleteFileETag(fileName: String): Boolean {
        val eTagFile = getETagFile(fileName)
        if (eTagFile.exists()) {
            return eTagFile.delete()
        }
        return false
    }

    override fun deleteFileETags() {
        if (eTagDir.exists()) {
            eTagDir.deleteRecursively()
            eTagDir.mkdir()
        }
    }
}
