/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudositereputation.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.numerics.shouldBeGreaterThanOrEqual
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test the operation of [DefaultStorageProvider] under Robolectric.
 *
 * @since 2021-01-04
 */
@RunWith(RobolectricTestRunner::class)
class StorageProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storageProvider = DefaultStorageProvider(context)
    private val fileName = "myFile"
    private val testData = "hello world"
    private val eTag = "eTag42"

    @Before
    fun init() {
        storageProvider.deleteFiles()
    }

    @Test
    fun checkReadWriteDelete() = runBlocking<Unit> {
        with(storageProvider) {
            getFile(fileName).exists() shouldBe false
            read(fileName) shouldBe null
            delete(fileName) shouldBe false

            readFileETag(fileName) shouldBe null
            deleteFileETag(fileName) shouldBe false

            write(fileName, testData.toByteArray())
            val file = getFile(fileName)
            file.exists() shouldBe true
            file.lastModified() shouldBeGreaterThanOrEqual 0L

            val content = read(fileName)
            content shouldNotBe null
            String(content!!) shouldBe testData

            writeFileETag(fileName, eTag)
            readFileETag(fileName) shouldBe eTag
            deleteFileETag(fileName) shouldBe true

            delete(fileName) shouldBe true
            read(fileName) shouldBe null
            delete(fileName) shouldBe false
            getFile(fileName).exists() shouldBe false

            readFileETag(fileName) shouldBe null
            deleteFileETag(fileName) shouldBe false
        }
    }

    @Test
    fun checkDeleteAll() = runBlocking<Unit> {
        with(storageProvider) {
            read(fileName) shouldBe null
            readFileETag(fileName) shouldBe null
            listFiles() shouldHaveSize 0

            write(fileName, testData.toByteArray())
            listFiles() shouldContainExactlyInAnyOrder listOf(fileName)

            writeFileETag(fileName, eTag)
            readFileETag(fileName) shouldBe eTag

            deleteFiles()
            deleteFileETags()

            readFileETag(fileName) shouldBe null
            read(fileName) shouldBe null
            listFiles() shouldHaveSize 0
        }
    }
}
