package me.anno.tests.assimp

import me.anno.ecs.prefab.PrefabCache
import me.anno.tests.LOGGER
import me.anno.utils.OS

fun main() {

    // to do compare the two models
    // to do their rendered result must be identical!

    val fbxPath = OS.downloads.getChild("3d/trooper fbx/source/silly_dancing.fbx")
    val glbPath = OS.downloads.getChild("3d/trooper gltf/scene.gltf")

    val fbx = PrefabCache[fbxPath]!!.getSampleInstance()
    val glb = PrefabCache[glbPath]!!.getSampleInstance()

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

    LOGGER.info(fbx)
    LOGGER.info(glb)
}