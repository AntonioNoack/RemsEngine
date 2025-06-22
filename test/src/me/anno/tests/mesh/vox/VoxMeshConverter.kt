package me.anno.tests.mesh.vox

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.async.Callback.Companion.mapCallback

/**
 * load all MagicaVoxel samples, and export them as binary GLTF files
 * */
fun main() {
    OfficialExtensions.initForTests()
    // test whether all vox meshes can be read normally
    // export vox meshes as normal meshes
    val source = downloads.getChild("MagicaVoxel/vox")
    val destination = desktop.getChild("vox2glb")
    destination.tryMkdirs()
    source.listChildren()
        .filter { it.lcExtension == "vox" }
        .mapCallback({ _, file, cb ->
            val scene = PrefabCache[file].waitFor()?.sample as Entity
            GLTFWriter().write(scene, destination.getChild("${file.nameWithoutExtension}.glb"), cb)
        }, { _, err ->
            err?.printStackTrace()
            Engine.requestShutdown()
        })
}