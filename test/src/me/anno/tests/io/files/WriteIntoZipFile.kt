package me.anno.tests.io.files

import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.zip.InnerZipFileV2
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertIs
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun main() {

    val source = desktop.getChild("test.zip")
    source.delete()

    // todo bug: if second.txt is placed in a subfolder, it doesn't appear in the result :/
    val firstName = "first.txt"
    val secondName = "second.txt"
    val thirdName = "third.txt"

    // create initial zip
    source.outputStream().use { fos ->
        ZipOutputStream(fos).use { zos ->
            val entry = ZipEntry(firstName)
            zos.putNextEntry(entry)
            zos.write("Hello World".encodeToByteArray())
            zos.closeEntry()
        }
    }

    InnerFolderCache.registerSignatures("zip", InnerZipFileV2::createZipFile)

    InnerZipFileV2.createZipFile(source, Callback.onSuccess { zipSource ->

        val first = zipSource.getChild(firstName)
        println("first.txt: ${first.readTextSync()}")

        val second = zipSource.getChild(secondName)
        assertIs(InnerZipFileV2::class, second.resolved())
        assertFalse(second.exists)
        assertFalse(second.isDirectory)
        second.writeText("Buenos Dias!")

        assertTrue(second.exists)
        assertFalse(second.isDirectory)
        println("second.txt: ${second.readTextSync()}")

        val third = zipSource.getChild(thirdName)

        (zipSource as InnerZipFileV2).fileSystem.invalidate()

        assertTrue(first.exists)
        assertFalse(first.isDirectory)

        assertTrue(second.exists)
        assertFalse(second.isDirectory)

        assertFalse(third.exists)
        assertFalse(third.isDirectory)

        println(zipSource.listChildren())

    })
}