package me.anno.tests.mesh

import me.anno.ecs.components.mesh.utils.IndexRemover.removeIndices
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val shape = flatCube.deepClone()
    shape.normals = null
    shape.removeIndices()
    shape.calculateNormals(true)
    testSceneWithUI("Smoothed Cube", shape)
}