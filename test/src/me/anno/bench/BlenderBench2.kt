package me.anno.bench

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.OfficialExtensions
import me.anno.gpu.texture.TextureCache
import me.anno.graph.hdb.HDBKey.Companion.InvalidKey
import me.anno.image.ImageCache
import me.anno.image.thumbs.AssetThumbnails
import me.anno.jvm.HiddenOpenGLContext
import me.anno.mesh.blender.BlenderReader
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager

fun main() {
    // benchmark reading blender files, so we can make it faster
    //  -> reading files is quick, takes only 0.15s for a 393 MB file, which is fine imo (2.6 GB/s)
    HiddenOpenGLContext.createOpenGL()
    OfficialExtensions.initForTests()
    val clock = Clock("BlenderBench2")
    LogManager.disableLoggers("BlenderShaderTree,BlenderFile,BlockTable")
    val source = downloads.getChild("3d/TheJunkShop/The Junk Shop.blend")
    source.readByteBuffer(true, Callback.onSuccess { buffer ->
        clock.benchmark(1, 50, "Loading") {
            val folder = BlenderReader.readAsFolder(source, buffer)
            val prefab = (folder.getChild("Scene.json") as PrefabReadable).readPrefab()
            val scene = prefab.getSampleInstance() as Entity
            var done = false
            AssetThumbnails.generateEntityFrame(source, InvalidKey, 64, scene) { result, exc ->
                done = true
                result?.destroy()
                exc?.printStackTrace()
            }
            waitUntil(true) { done }
            // reset caches
            ImageCache.clear()
            TextureCache.clear()
        }
    })
}