package me.anno.tests.assimp

import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ECSRegistry
import me.anno.tests.LOGGER
import me.anno.utils.OS.downloads

fun main() {

    // now all works :)

    ECSRegistry.init()

    for (file in listOf(
        "3d/bunny.obj",
        "3d/azeria/scene.gltf",
        "3d/azeria/azeria.zip/scene.gltf"
    )) {
        MeshCache.getEntry(downloads.getChild(file))!!
        LOGGER.info("done :)")
    }
}