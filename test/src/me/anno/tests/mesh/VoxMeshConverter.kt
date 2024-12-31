package me.anno.tests.mesh

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache.getPrefabSampleInstance
import me.anno.engine.OfficialExtensions
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

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
    for (file in source.listChildren()) {
        if (file.lcExtension != "vox") continue
        val scene = getPrefabSampleInstance(file) as Entity
        GLTFWriter().write(scene, destination.getChild("${file.nameWithoutExtension}.glb"))
    }
    Engine.requestShutdown()
}