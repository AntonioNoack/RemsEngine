package me.anno.tests.assimp

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.StaticMeshesLoader
import me.anno.utils.LOGGER
import me.anno.utils.OS

fun main() {

    // to do compare the two models
    // to do their rendered result must be identical!

    val fbxPath = getReference(OS.downloads, "3d/trooper fbx/source/silly_dancing.fbx")
    val glbPath = getReference(OS.downloads, "3d/trooper gltf/scene.gltf")

    val fbx = StaticMeshesLoader.load(fbxPath)
    val glb = StaticMeshesLoader.load(glbPath)

    /*fbx.bones.forEachIndexed { index,it ->
        LOGGER.info("$index ${it.name}")
        LOGGER.info(it.offsetMatrix)
        // LOGGER.info(it.skinningMatrix)
    }

    glb.bones.forEachIndexed { index,it ->
        LOGGER.info("$index ${it.name}")
        LOGGER.info(it.offsetMatrix)
        // LOGGER.info(it.skinningMatrix)
    }*/

    LOGGER.info(fbx.hierarchy)
    LOGGER.info(glb.hierarchy)

}