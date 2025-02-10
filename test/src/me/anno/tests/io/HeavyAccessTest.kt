package me.anno.tests.io

import me.anno.engine.OfficialExtensions
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class HeavyAccessTest {

    // create heavy zip file
    // request access to it from 10 places
    // ensure all get access quickly
    // ensure none is starved
    // ensure it gets opened a single time only

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testAccess() {
        OfficialExtensions.initForTests() // register Unpack module
        InnerFolderCache.sizeLimit = 10_000
        val numFiles = 10
        val fileSize = 25_000
        val random = Random(1234)
        val entries = (0 until numFiles)
            .map { "$it.bin" to random.nextBytes(fileSize) }
        val rawZipBytes = ByteArrayOutputStream()
        ZipOutputStream(rawZipBytes).use { zos ->
            for ((name, bytes) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
            }
        }
        val heavyFile = InnerTmpByteFile(rawZipBytes.toByteArray())
        for ((name, bytes) in entries) {
            heavyFile.getChild(name).readBytes { bytes1, err ->
                assertNull(err)
                assertContentEquals(bytes, bytes1)
                println("Got correct bytes for $name on ${Thread.currentThread().name}")
            }
        }
    }
}