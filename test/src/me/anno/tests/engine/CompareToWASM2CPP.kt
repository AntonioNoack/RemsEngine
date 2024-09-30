package me.anno.tests.engine

import me.anno.ecs.components.mesh.shapes.IcosahedronModel.createIcosphere
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    testSceneWithUI("CompareToWASM2CPP", createIcosphere(1))
}