package me.anno.tests.assimp

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.maths.Maths.pow
import me.anno.tests.LOGGER
import me.anno.utils.OS.downloads

fun main() {

    // todo load multiple gltf and fbx files, and find their scale correctly

    OfficialExtensions.initForTests()
    val files = listOf(
        downloads.getChild("3d/Lucy0.fbx"), // 100x too large
        downloads.getChild("3d/robot_kyle_walking.fbx"), // 100x too large
        downloads.getChild("3d/azeria/scene.gltf"), // 100x too large
        // downloads.getChild("3d/BrainStem.glb"), // correct (from the beginning)
    )

    ECSRegistry.initMeshes()
    for (file in files) {
        val bounds = MeshCache[file, false]!!.getBounds()
        if (bounds.volume > pow(50f, 3f)) {
            LOGGER.warn("$file is incorrect: $bounds")
        }
    }

    Engine.requestShutdown()

}