package me.anno.tests.assimp

import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.utils.LOGGER
import me.anno.utils.OS

fun main() {

    val file = OS.documents.getChild("CuteGhost.fbx")
    val model = AnimatedMeshesLoader.load(file)

    // LOGGER.info(model.bones)

    LOGGER.info(model.hierarchy.toStringWithTransforms(0))

}