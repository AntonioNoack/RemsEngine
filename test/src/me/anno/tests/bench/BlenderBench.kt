package me.anno.tests.bench

import me.anno.mesh.blender.BlenderReader
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import org.apache.logging.log4j.LogManager

fun main() {
    // benchmark reading blender files, so we can make it faster
    //  -> reading files is quick, takes only 0.15s for a 393 MB file, which is fine imo (2.6 GB/s)
    val clock = Clock("BlenderBench")
    LogManager.disableLogger("BlenderShaderTree")
    LogManager.disableLogger("BlenderFile")
    LogManager.disableLogger("BlockTable")
    val source = downloads.getChild("The Junk Shop.blend")
    val bytes = source.readByteBufferSync(true)
    clock.benchmark(1, 50, "Loading") {
        BlenderReader.readAsFolder(source, bytes)
    }
}