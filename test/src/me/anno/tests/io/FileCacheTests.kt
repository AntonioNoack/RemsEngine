package me.anno.tests.io

import me.anno.cache.FileCache
import me.anno.io.files.FileReference
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class FileCacheTests {

    companion object {
        private const val TIMEOUT_MILLIS = 10L
    }

    class FileCacheImpl : FileCache<String, String>(
        "cache.json", "cache", "test"
    ) {

        fun getValue(src: String, async: Boolean): String? {
            return getEntry(src, TIMEOUT_MILLIS, async, ::generateFile)?.value
        }

        override fun getUniqueFilename(key: String): String = key.toAllowedFilename()!!

        override fun load(key: String, src: FileReference?): String {
            src ?: return ""
            return src.readTextSync()
        }

        override fun fillFileContents(
            key: String,
            dst: FileReference,
            onSuccess: () -> Unit,
            onError: (Exception?) -> Unit
        ) {
            dst.writeText(key)
            onSuccess()
        }
    }

    private fun init(): FileCacheImpl {
        val tested = FileCacheImpl()
        tested.init()
        // reset
        tested.cacheFolder.delete()
        tested.cacheFolder.tryMkdirs()
        return tested
    }

    private fun finish(tested: FileCacheImpl) {
        tested.cacheFolder.delete()
    }

    @Test
    fun testFileCacheFirstAsyncNull() {
        val fileCache = init()
        // first access should fail when async
        assertEquals(null, fileCache.getValue("abc", true))
        finish(fileCache)
    }

    @Test
    fun testFileCacheAsyncIsLoading() {
        val fileCache = init()
        assertEquals("abc", Sleep.waitUntilDefined(true) {
            fileCache.getValue("abc", true)
        })
        finish(fileCache)
    }

    @Test
    fun testFileCacheFirstSync() {
        val fileCache = init()
        // first sync access should
        assertEquals("abc", fileCache.getValue("abc", false))
        // second access should even return a result if async
        assertEquals("abc", fileCache.getValue("abc", true))
        finish(fileCache)
    }

    @Test
    fun testFileCacheUpdateAndReloading() {
        val fileCache = init()
        // load the value
        assertEquals("abc", fileCache.getValue("abc", false))

        Thread.sleep(TIMEOUT_MILLIS + 5)
        // no update happened -> the value still should be present
        assertEquals("abc", fileCache.getValue("abc", true))

        Thread.sleep(TIMEOUT_MILLIS + 5)
        fileCache.update()
        // an update happened -> the value should be lost now
        assertEquals(null, fileCache.getValue("abc", true))

        // but it should be reloadable
        assertEquals("abc", fileCache.getValue("abc", false))
        finish(fileCache)
    }
}