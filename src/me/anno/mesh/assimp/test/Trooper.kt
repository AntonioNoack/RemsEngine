package me.anno.mesh.assimp.test

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.utils.LOGGER
import me.anno.utils.OS

fun main() {

    // todo compare the two models
    // todo their rendered result must be identical!

    val fbxPath = getReference(OS.downloads, "3d/trooper fbx/source/silly_dancing.fbx")
    val glbPath = getReference(OS.downloads, "3d/trooper gltf/scene.gltf")

    val fbx = AnimatedMeshesLoader.load(fbxPath)
    val glb = AnimatedMeshesLoader.load(glbPath)

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