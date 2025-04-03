package me.anno.tests.mesh

import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // for comparison with JVM2WASM
    testSceneWithUI("Icosphere-3", IcosahedronModel.createIcosphere(3))
}