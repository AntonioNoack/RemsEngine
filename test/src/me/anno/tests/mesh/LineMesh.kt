package me.anno.tests.mesh

import me.anno.ecs.components.mesh.LineMesh
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

/**
 * Create a line mesh from a regular mesh, and check whether it looks correctly.
 * */
fun main() {
    val mesh = IcosahedronModel.createIcosphere(2)
    val lineMesh = LineMesh(mesh)
    testSceneWithUI("LineMesh", lineMesh.ref)
}