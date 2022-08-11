package me.anno.tests.mesh

import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference
import me.anno.mesh.obj.OBJReader
import me.anno.mesh.obj.UVCorrection
import me.anno.utils.Clock
import me.anno.utils.OS

/**
 * uv-sign detection implementation test
 * */
fun main() {
    ECSRegistry.init()
    @Suppress("SpellCheckingInspection")
    val samples = listOf(
        // path and ideal detection
        "ogldev-source/Content/jeep.obj", // y
        "ogldev-source/Content/hheli.obj", // y
        "ogldev-source/Content/spider.obj", // n
        "ogldev-source/Content/dragon.obj", // doesn't matter
        "ogldev-source/Content/buddha.obj", // doesn't matter
        "ogldev-source/Content/dabrovic-sponza/sponza.obj"
    )
    val clock = Clock()
    for (sample in samples) {
        val ref = FileReference.getReference(OS.downloads, sample)
        val folder = OBJReader.readAsFolder(ref)
        clock.start()
        UVCorrection.correct(folder)
        clock.stop("calc") // the first one is always extra long
    }
}
