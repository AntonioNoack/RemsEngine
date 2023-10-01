package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val mesh = CylinderModel.createMesh(
        16, 3, true, true,
        listOf(
            Material().ref,
            Material().apply { diffuseBase.set(1f, 0f, 0f, 1f) }.ref,
            Material().apply { diffuseBase.set(0f, 0f, 1f, 1f) }.ref
        ),
        3f, Mesh()
    )
    testSceneWithUI("Cylinder", mesh)
}