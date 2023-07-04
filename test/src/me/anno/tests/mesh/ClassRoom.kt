package me.anno.tests.mesh

import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.AnimatedMeshesLoader

fun main() {
    ECSRegistry.initMeshes()
    AnimatedMeshesLoader.readAsFolder(getReference("C:/Users/Antonio/Downloads/3d/ClassRoom/classroom.glb"))
}