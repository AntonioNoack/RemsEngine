package me.anno.tests.io

import me.anno.engine.OfficialExtensions
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class HeavyAccessTest {

    companion object {
        private val LOGGER = LogManager.getLogger(HeavyAccessTest::class)
    }

    private class HeavyFile(private val content: () -> ByteArray) : InnerTmpFile("bin") {

        override fun readBytes(callback: Callback<ByteArray>) {
            callback.ok(content())
        }

        override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
            callback.ok(content().inputStream())
        }

        override fun length(): Long {
            return Int.MAX_VALUE.toLong()
        }
    }

    fun testAccessImpl(isLightFiles: Boolean, limit: Int) {
        OfficialExtensions.initForTests() // register Unpack module
        InnerFolderCache.sizeLimit = 10_000

        val numFiles = 10
        val fileSize = if (isLightFiles) 5_000 else 25_000

        val random = Random(1234)
        val entries = (0 until numFiles)
            .map { "$it.bin" to random.nextBytes(fileSize) }

        val rawZipByteStream = ByteArrayOutputStream()
        ZipOutputStream(rawZipByteStream).use { zos ->
            for ((name, bytes) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
            }
        }

        val rawZipBytes = rawZipByteStream.toByteArray()
        var readCounter = 0
        val heavyFile = HeavyFile {
            readCounter++
            rawZipBytes
        }

        for ((name, bytes) in entries) {
            heavyFile.getChild(name).readBytes { bytes1, err ->
                assertNull(err)
                assertContentEquals(bytes, bytes1)
                LOGGER.info("Got correct bytes for $name on ${Thread.currentThread().name}")
            }
        }

        // once for signature, once for finding all children & reading the contents
        // maybe a second time for reading the contents, if there is a delay
        assertTrue(readCounter <= limit)
    }

    /**
     * - create heavy zip file
     * - request access to it from 10 places
     * - ensure all get access quickly
     * - ensure none is starved
     * - ensure it gets opened two times only for heavy files
     * */
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testHeavyAccess() {
        testAccessImpl(false, 3)
    }

    /**
     * - create heavy zip file
     * - request access to it from 10 places
     * - ensure all get access quickly
     * - ensure none is starved
     * - ensure it gets opened a single time only for light files
     * */
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testLightFiles() {
        testAccessImpl(true, 2)
    }
}