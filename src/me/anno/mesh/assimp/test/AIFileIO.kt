package me.anno.mesh.assimp.test

import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.StaticMeshesLoader
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads

fun main() {

    // now all works :)

    ECSRegistry.initNoGFX()

    for (file in listOf(
        "3d/bunny.obj",
        "3d/azeria/scene.gltf",
        "3d/azeria/azeria.zip/scene.gltf"
    )) {
        val fileI = getReference(downloads, file)
        StaticMeshesLoader().read(fileI, fileI.getParent()!!)
        LOGGER.info("done :)")
    }

}