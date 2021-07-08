package me.anno.mesh.assimp.test

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.utils.OS

fun main() {

    // todo compare the two models
    // todo their rendered result must be identical!

    val fbxPath = getReference(OS.downloads, "3d/trooper fbx/source/silly_dancing.fbx")
    val glbPath = getReference(OS.downloads, "3d/trooper gltf/scene.gltf")

    val fbx = AnimatedMeshesLoader.load(fbxPath)
    val glb = AnimatedMeshesLoader.load(glbPath)

    /*fbx.bones.forEachIndexed { index,it ->
        println("$index ${it.name}")
        println(it.offsetMatrix)
        // println(it.skinningMatrix)
    }

    glb.bones.forEachIndexed { index,it ->
        println("$index ${it.name}")
        println(it.offsetMatrix)
        // println(it.skinningMatrix)
    }*/

    println(fbx.hierarchy)
    println(glb.hierarchy)

}