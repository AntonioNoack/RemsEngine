package me.anno.tests.assimp

import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ECSRegistry
import me.anno.maths.Maths.pow
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads

fun main() {

    // todo load multiple gltf and fbx files, and find their scale correctly

    val files = listOf(
        downloads.getChild("3d/lucy.fbx"), // 100x too large
        downloads.getChild("3d/robot_kyle_walking.fbx"), // 100x too large
        downloads.getChild("3d/azeria/scene.gltf"), // 100x too large
        // downloads.getChild("3d/BrainStem.glb"), // correct (from the beginning)
    )

    ECSRegistry.initMeshes()
    for (file in files) {
        val bounds = MeshCache[file, false]!!.ensureBounds()
        if (bounds.volume() > pow(50f, 3f)) {
            LOGGER.warn("$file is incorrect: $bounds")
        }
    }

}