package me.anno.tests.bench

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.ECSRegistry
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.graph.hdb.HDBKey.Companion.InvalidKey
import me.anno.image.ImageCPUCache
import me.anno.image.ImageGPUCache
import me.anno.io.files.thumbs.Thumbs
import me.anno.mesh.blender.BlenderReader
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep.waitUntil
import org.apache.logging.log4j.LogManager

fun main() {
    // benchmark reading blender files, so we can make it faster
    //  -> reading files is quick, takes only 0.15s for a 393 MB file, which is fine imo (2.6 GB/s)
    HiddenOpenGLContext.createOpenGL()
    ECSRegistry.initPrefabs()
    ECSRegistry.initMeshes()
    ECSRegistry.initLights()
    val clock = Clock()
    LogManager.disableLogger("BlenderShaderTree")
    LogManager.disableLogger("BlenderFile")
    LogManager.disableLogger("BlockTable")
    val source = downloads.getChild("The Junk Shop.blend")
    val bytes = source.readByteBufferSync(true)

    // todo there isn't really any reasons for the ImageCPU cache to be active... is there?
    //  -> if it is accessed, die
    clock.benchmark(1, 50, "Loading") {
        val folder = BlenderReader.readAsFolder(source, bytes)
        val prefab = (folder.getChild("Scene.json") as PrefabReadable).readPrefab()
        val scene = prefab.getSampleInstance() as Entity
        var done = false
        Thumbs.generateEntityFrame(source, InvalidKey, 64, scene) { result, exc ->
            done = true
            result?.destroy()
            exc?.printStackTrace()
        }
        waitUntil(true) { done }
        // reset caches
        ImageCPUCache.clear()
        ImageGPUCache.clear()
    }
}