package me.anno.tests.engine

import me.anno.ecs.components.mesh.LODMeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val mesh = LODMeshComponent()
    mesh.meshes = (3 downTo 0).map { subDivisions ->
        IcosahedronModel.createIcosphere(subDivisions).ref
    }
    testSceneWithUI("LodMesh", mesh)
}