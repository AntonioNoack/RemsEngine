package me.anno.tests.mesh

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.smoothCube
import org.joml.Vector3f

/**
 * test the normals generated on a flattened box:
 * large faces shall be prioritized
 * */
fun main() {
    val mesh = smoothCube.scaled(Vector3f(3f, 0.1f, 5f)).front
    mesh.calculateNormals(smooth = true)
    testSceneWithUI("Smooth Cube Normals", mesh)
}