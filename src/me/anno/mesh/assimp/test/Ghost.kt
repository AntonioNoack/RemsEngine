package me.anno.mesh.assimp.test

import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.utils.OS

fun main() {

    val file = OS.documents.getChild("CuteGhost.fbx")
    val model = AnimatedMeshesLoader.load(file)

    println(model.bones)

    println(model.hierarchy.toStringWithTransforms(0))

}