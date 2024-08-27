package me.anno.tests.tools

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.structures.Recursion
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val LOGGER = LogManager.getLogger("CompactZips")

fun main() {
    OfficialExtensions.initForTests()
    val src = getReference("E:/Assets/Quaternius")
    for (file in src.listChildren()) {
        compactZip(file)
    }
    Engine.requestShutdown()
}

/**
 * if the file contains exactly one folder, replace it with those contents
 * */
fun compactZip(ref: FileReference) {
    if (!ref.exists || ref.isDirectory) return
    val src = ref.listChildren()
    if (src.size == 1 && src[0].isDirectory) {
        LOGGER.info("Processing $ref")
        val prefix = "${src[0].absolutePath}/"
        // todo it probably would be nice to have a standard function to create packages,
        //  and let the standard implementation be easy without compression like .tar
        // first buffer everything: we don't want to delete the floor under our feet
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)
        Recursion.processRecursive(src[0]) { file, remaining ->
            when (file) {
                is InnerFolder -> remaining.addAll(file.listChildren())
                is InnerFile -> {
                    if (file.absolutePath.startsWith(prefix)) {
                        zos.putNextEntry(ZipEntry(file.absolutePath.substring(prefix.length)))
                        zos.write(file.readBytesSync())
                        zos.closeEntry()
                    } else LOGGER.warn("Incorrect name ${file.absolutePath}")
                }
            }
        }
        zos.close()
        ref.outputStream().use { ros ->
            bos.writeTo(ros)
        }
    } else LOGGER.warn("Ignored $ref")
}