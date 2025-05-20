package me.anno.tests.bench

import me.anno.mesh.blender.BlenderReader
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager

fun main() {
    // benchmark reading blender files, so we can make it faster
    //  -> reading files is quick, takes only 0.14s for a 393 MB file, which is fine imo (2.6 GB/s)
    val clock = Clock("BlenderBench")
    LogManager.disableLoggers("BlenderShaderTree,BlenderFile,BlockTable")
    val source = downloads.getChild("3d/TheJunkShop/The Junk Shop.blend")
    source.readByteBuffer(true, Callback.onSuccess { buffer ->
        clock.benchmark(1, 50, "Loading") {
            BlenderReader.readAsFolder(source, buffer)
        }
    })
}