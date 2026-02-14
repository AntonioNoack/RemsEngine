package me.anno.tests.mesh.shapes

import me.anno.ecs.components.mesh.shapes.RingMeshModel.createRingMesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    testSceneWithUI("RingMesh", createRingMesh(20, 0.5f, 1f))
}