package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.fbx.FBX6000.readBinaryFBX6000AsMeshes
import me.anno.utils.OS.desktop

fun main() {
    val source = desktop.getChild("SM_Env_Flag_Long_Germany_A.fbx")
    println(AnimatedMeshesLoader.readAsFolder(source))
    val meshes = readBinaryFBX6000AsMeshes(source.inputStreamSync())
    val entity = Entity()
    for (mesh in meshes) entity.add(MeshComponent(mesh))
    testSceneWithUI(entity)
}